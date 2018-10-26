package main

import(
	"bufio"
	"net"
	"net/http"
	//"runtime"
	"log"
	"io"
	"fmt"
	"time"
	//"os"
	"encoding/json"
	"strings"
 	//"reflect"
	//"hash/crc32"
	"golang.org/x/net/websocket"
)

func handleRequest(w http.ResponseWriter,req* http.Request){
	//io.WriteString(w,"helloworld!")

	//s, _ := ioutil.ReadAll(r.Body) //把  body 内容读入字符串 s
    //fmt.Fprintf(w, "%s", s)        //在返回页面中显示内容。
    //fmt.Println("程序已退出.")

    info := fmt.Sprintln("URL", req.URL, "HOST", req.Host, "Method", req.Method, "RequestURL", req.RequestURI, "RawQuery", req.URL.RawQuery)
    fmt.Fprintln(w, info)
}

//娃娃机信息简单版。
type MachineInfo struct{
	//MAC地址:唯一标识一台娃娃机。这是来自于安卓板的以太网端口。你应该以此作为数据库的主键字段
	mac string//identify for wawa machine.
	
	//名称(方便管理娃娃机的。并无实际意义，你可以把里面放的娃娃作为该娃娃机的名称，方便你看到就知道是哪台娃娃机)
	//name string//for people use.

	//标识当前娃娃机状态：在线 离线 故障 正忙。你应该处理好这些状态切换。比如有玩家在玩的时候，你应该标识为正忙，并且通知这个房间里面的所有人，如果要玩，请预约。并返回排队的人数
	//state int//free busying offline//joke. this sample is without database.so ,let the offline machine goto hell. but, you can't do it like me .
	
	//房间里面的用户。这个简单版的服务器没有用到。事实上，你应该将进入此房间的用户想办法保存起来，然后娃娃机状态变更时，通知他们
	//var Map<Socket, Integer> user_in_room;//
	
	//实际通信的socket句柄。你懂的。
	socket net.Conn//socket for machine.
	
	//正在玩的玩家socket。简单版的服务器存的是socket。因为我并不知道用什么来标识用户。目前没有登录，没有用户ID。所以只能用socket。实际应该是用户ID。然后根据此ID找到socket执行转发
	current_player net.Conn
	
	//上次心跳时间。用来检查心跳超时
	last_heartbeattime int64//30s not hear beat. dead. but i don't do this logic to make this code simple.you do it yourself.
	
	//发送给娃娃机的次数
	//sendCount int64
	
	//娃娃机接收到的次数
	//recvCount int64
}

 
var all_machines map[string]MachineInfo
var packet_id int

func main() {
	//runtime.GOMAXPROCS(1) //限制一个1核上
	fmt.Println("启动服务器...")
	//娃娃机连接到此端口
	all_machines=map[string]MachineInfo{}
	go BeginWaWaJi()
	go CheckWawajiAlive()

	//tcp客户端
	go BeginPlayer()

	//websocket客户端
	http.Handle("/websocket", websocket.Handler(Echo))

	//处理http请求。这里用来返回扫码结果
	fmt.Println("http begin.")
	http.HandleFunc("/",handleRequest)
    err:=http.ListenAndServe(":8088",nil)
	if err!=nil{
		log.Fatal("ListenAndServe:",err)
	}

	fmt.Println("程序已退出.")
}

func BeginWaWaJi(){
	listener, _ := net.Listen("tcp", ":7770")

	for {
		conn, _ := listener.Accept() // 持续监听客户端连接
		fmt.Println("a machine has connected.")
		go WawajiLogic(conn)
	}
}

func CheckWawajiAlive(){
	for{
			time.Sleep(time.Duration(5)*time.Second)

			for k, v := range all_machines {
	   			if (time.Now().Unix() - v.last_heartbeattime) > 30 {
	   			fmt.Println("timeout remove", v.mac)
	   			delete(all_machines, k)
	   		}
		}
	}
}

func typeof(v interface{}) string {
    return fmt.Sprintf("%T", v)
}

func WawajiLogic(conn net.Conn){
	g_cache_buff := make([]byte, 4096)
	var cur_read_index int//当前处理的索引缓冲
	var cur_write_index int//当前可写入位置的索引
	var fill_count int//当前缓冲已填入数据个数
	var strMYMAC string

 	buf := make([]byte, 4096)
 	for{
		n,err := bufio.NewReader(conn).Read(buf)
		if err != nil {
			if err == io.EOF {
		        fmt.Printf("client %s is close!\n",conn.RemoteAddr().String())
		    }

		    //关闭连接
			conn.Close()
	    	return
	     }

		//fmt.Printf("recv len:%d", n)
		//fmt.Printf(time.Now().Format("[2006-01-02 15:04:05]"))
		//for i:=0;i<n;i++{
		//	fmt.Printf("%02x", buf[i])
	    //}
	    //fmt.Println();
	    //处理拼包拆包。
	    //将收到的数据放入缓冲区中
	    len1 := n;
		for iIndex := 0; iIndex < len1; iIndex++ {
			g_cache_buff[cur_write_index] = buf[iIndex]
			fill_count++
		if fill_count >= 4096 {
			fmt.Println("com data out of buff ! warining! data will be overwrite lost!!!")
		}

		cur_write_index++
		if cur_write_index >= 4096{
			cur_write_index = 0
		}
	}

	for{ 
		if fill_count <9 {
			break
		}

		//寻找到第一位0xfe如果找不到 跳过这个数据
		if  g_cache_buff[cur_read_index] != 0xfe  {
			fill_count--
			cur_read_index++
			if cur_read_index >= 4096{
				cur_read_index = 0
			}
			continue
		}

		if g_cache_buff[cur_read_index] == 0xfe{	//说明用的是我们的协议 等待包完整后转发
			//现在已经有了足够的数据。
			//校验数据如果不成功 则还是跳过这个fe
			 a0 := g_cache_buff[cur_read_index] & 0xff;
			 a3 := (^g_cache_buff[(cur_read_index + 3) % 4096])& 0xff;

			 a1 := g_cache_buff[(cur_read_index + 1) % 4096] & 0xff;
			 a4 := (^g_cache_buff[(cur_read_index + 4) % 4096])& 0xff;

			 a2 := g_cache_buff[(cur_read_index + 2) % 4096] & 0xff;
			 a5 := (^g_cache_buff[(cur_read_index + 5) % 4096])& 0xff;

			//校验头不成功 跳过这个fe
			if (a0 != a3) || (a1 != a4) || (a2 != a5){
				fill_count--
				cur_read_index++
				if cur_read_index >= 4096{
					cur_read_index = 0
				}
				continue
			}

			len1 := g_cache_buff[(cur_read_index + 6) % 4096]
			if fill_count < int(len1){		//接收的数据长度还不够 不处理
				break
			}

			//printf("check suming\r\n");

			//检查校验和 不成功 还是跳过这个fe
			var sum int;
			for kk := 6; kk < int(len1 - 1); kk++ {
				sum += int(g_cache_buff[(cur_read_index + kk) % 4096]);
			}
			sum = sum % 100;
			if sum != int(g_cache_buff[(cur_read_index + int(len1) - 1) % 4096]) {
				fill_count--
				cur_read_index++
				if  cur_read_index >= 4096{
					cur_read_index = 0
				}

				continue
			}

			cmd := g_cache_buff[(cur_read_index + 7) % 4096] & 0xff
			if cmd == 0x35{
				tmp := make([]byte, len1)
				mac := make([]byte, len1-9)
				for kk := 0; kk< int(len1); kk++{
					tmp[kk] = g_cache_buff[(cur_read_index + kk)% 4096]
					if kk>=8 && kk<int(len1-1){
						mac[kk-8] = tmp[kk]
					}
				}
				conn.Write(tmp)

				var str string = string(mac[:])
				strMYMAC = str
				//fmt.Println(str, "心跳。返回")
				if _, ok := all_machines[str]; ok {
					t := all_machines[str]
					t.last_heartbeattime = time.Now().Unix()
					all_machines[str] = t
				}else{
					all_machines[str] = MachineInfo{mac:str,socket:conn,current_player:nil,last_heartbeattime:time.Now().Unix()}
				}
			}else if cmd == 0x31{//给客户端发送开局成功的json
				type RetS struct{
					Cmd string `json:"cmd"`
					Ret int `json:"ret"`
				}

				var rt = RetS{Cmd:"start_game",Ret:1}
				jsRet,err := json.Marshal(rt)                                                             
				if err != nil{                                                                    
					fmt.Println("err 2", err)                                                       
				}                                                                                    
				//fmt.Printf("%s\n",jsRet)

				conntype :=typeof(all_machines[strMYMAC].current_player)
				if(strings.Contains(conntype, "websocket")){
					all_machines[strMYMAC].current_player.Write(jsRet)
				}else{
				da_len := len(jsRet);
				ret_data := make([]byte, 3)
				ret_data[0] = 0xda
				ret_data[1] = byte(da_len/256)
				ret_data[2] = byte(da_len%256)
				ret_data = append(ret_data, jsRet...)
				fmt.Println(string(ret_data));
				all_machines[strMYMAC].current_player.Write(ret_data)
				}


				//fmt.Println("conn type:"+typeof(all_machines[strMYMAC].current_player))
			}else if cmd == 0x33{
				//{"cmd":"game_ret","ret":0}
				type RetGame struct{
					Cmd string `json:"cmd"`
					Ret int `json:"ret"`
				}

				var rt = RetGame{Cmd:"game_ret",Ret:int(g_cache_buff[(cur_read_index + 8) % 4096] & 0xff)}
				jsRet,err := json.Marshal(rt)                                                             
				if err != nil{                                                                    
					fmt.Println("err 2", err)                                                       
				}                                                                                    
				//fmt.Printf("%s\n",jsRet)

				conntype :=typeof(all_machines[strMYMAC].current_player)
				if(strings.Contains(conntype, "websocket")){
					all_machines[strMYMAC].current_player.Write(jsRet)
				}else{
					da_len := len(jsRet);
					ret_data := make([]byte, 3)
					ret_data[0] = 0xda
					ret_data[1] = byte(da_len/256)
					ret_data[2] = byte(da_len%256)
					ret_data = append(ret_data, jsRet...)
					fmt.Println(string(ret_data));
					all_machines[strMYMAC].current_player.Write(ret_data)
				}
			}else {
				var tmp[250] byte
				for kk := 0; kk< int(len1); kk++{
					tmp[kk] = g_cache_buff[(cur_read_index + kk)% 4096]
				}
			}

			fill_count -= int(len1)
			cur_read_index = int((cur_read_index + int(len1)) % 4096);
		}//end of if( == 0xfe)
	}//end of while


	    //if buf[7] == 0x35 
		//fragment := make([]byte, n)
		//		copy(fragment, buf[:n])
		//		conn.Write(fragment)
	
 	}

	conn.Close()
}

//==============处理玩家连接和操作转发
func BeginPlayer(){
	listener, _ := net.Listen("tcp", ":7771")

	for {
		conn, _ := listener.Accept() // 持续监听客户端连接
		fmt.Println("a player has connected.")
		go HandlePlayerTCP(conn)
	}
}

func make_com(nums ...byte) []byte {//
	//fmt.Println(len(nums))
	pack_len := len(nums)+8

	packet_id++
	
	pack := make([]byte, 7)
	pack[0] = 0xfe
	pack[1] = byte(packet_id/256)
	pack[2] = byte(packet_id%256)
	pack[3] = 1
	pack[4] = ^pack[1]
	pack[5] = ^pack[2]
	pack[6] = byte(pack_len)
	pack = append(pack, nums...)

	//fmt.Print(nums, " ")
	sum := 0
	for i := 6;i<len(pack);i++ {
	    sum += int(pack[i])
	}

	last_b := byte(sum%100)
	pack = append(pack, last_b)
	//fmt.Println(total)

	return pack
}
/*
DA
数据长度2位
json

{"cmd":"req_roomlist"}
{"cmd":"enter_room","mac":"XXXX"}
{"cmd":"exit_room"}
{"cmd":"start_game"}
{"cmd":"operation","type":0}
*/
func HandlePlayerTCP(conn net.Conn){
	//玩家过来的数据格式是 0xDA 数据长度2位 json
	//因为没有过长的包，所以不考虑拼包的情况.算法是先接3个字节，再获取一个缓冲，拿到剩下的字节即可。如果一个包真的分成2次发送。。。

	var inRoomMAC string //玩家客户端所在的娃娃机房间。通过这个key去查找它的socket。然后给它发消息

	head := make([]byte, 3)
	reader := bufio.NewReader(conn)
 	for{
		n,err := reader.Read(head)
		if err != nil {
			if err == io.EOF {
		        fmt.Printf("client %s is close!\n",conn.RemoteAddr().String())
		    }
		    //关闭连接
			conn.Close()
	    	return
	     }

	     if (n== 3) && (head[0]==0xDA){
	     	var data_len int
	     	data_len = int(head[1])*256&0xff + int(head[2])&0xff
	     	data_body := make([]byte, data_len)

	     	n,err := reader.Read(data_body)
			if err != nil {
				if err == io.EOF {
		        fmt.Printf("client %s is close!\n",conn.RemoteAddr().String())
		    	}
		    	fmt.Printf("err%d\n",err)
		    	//关闭连接
				conn.Close()
	    		return
	     	}

	     	if n < data_len{
	     		//包不完整。继续接受
	     		data_body = data_body[0:n]
	     		left_data_len := data_len -n
	     		fmt.Println("不完整包，继续接收:%d\n",left_data_len)
	     		for{
	     			left_data := make([]byte, left_data_len)
	     			n,err := reader.Read(left_data)

					if err != nil {
						if err == io.EOF {
							fmt.Printf("client %s is close!\n",conn.RemoteAddr().String())
						}
						//关闭连接
						conn.Close()
						return
					}

					left_data = left_data[0:n]
					data_body = append(data_body, left_data...)
					left_data_len -= n;

	     			if left_data_len<=0{
	     				break
	     			}
	     		}
	     	}

			var strJosn string = string(data_body[:])
	     	fmt.Println("收到玩家客户端命令"+strJosn)
	     	//解析出json指令根据指令执行操作
	     	//var obj interface{} // var obj map[string]interface{}
	     	//var dat map[string]interface{}
			//json.Unmarshal(data_body[:], &obj)
			//m := obj.(map[string]interface{})

			var dat map[string]interface{}
			json.Unmarshal(data_body[:], &dat)
			strCmd := dat["cmd"]

			if strCmd=="req_roomlist"{
				roomList := make([]string, len(all_machines))
				indexI := 0

			type RoomRet struct{
				Cmd string `json:"cmd"`
				Roomlist []string `json:"rooms"`
			}

			for k, _ := range all_machines {
	   			roomList[indexI] = k
	   			indexI++
			}

			var roList = RoomRet{Cmd:"reply_roomlist", Roomlist:roomList}
			b2,err2 := json.Marshal(roList)                                                             
			if err2 != nil{                                                                    
				fmt.Println("err 2", err)                                                       
			}                                                                                    
			fmt.Println(string(b2))


				//b,err := json.Marshal(roomList)                                                             
				//if err != nil{                                                                    
				//	fmt.Println("err 2", err)                                                       
				//}                                                                                    
				//fmt.Println(string(b))

				da_len := len(b2);
				ret_data := make([]byte, 3)
				ret_data[0] = 0xda
				ret_data[1] = byte(da_len/256)
				ret_data[2] = byte(da_len%256)
				ret_data = append(ret_data, b2...)

				conn.Write(ret_data)
			}else if strCmd=="enter_room"{
				strMAC:=dat["mac"]
				inRoomMAC = strMAC.(string)
			}else if strCmd == "start_game"{
				//当前玩家
				t1 := all_machines[inRoomMAC]
				t1.current_player = conn
				all_machines[inRoomMAC] = t1

				//下发指令到娃娃机
				com_cmd := make_com(0x31,60,0,0,0,0,0,0,0,0,0,0)
				all_machines[inRoomMAC].socket.Write(com_cmd)
			}else if strCmd == "operation"{
				abc := byte(dat["type"].(float64))
 	//dat["key"] = int(dat["type"].(float64))
    //val2 := dat["key"]
   	// fmt.Printf("%v, %v\n", val2, reflect.TypeOf(val2)) // 10, int
	
				//下发指令到娃娃机
				com_cmd := make_com(0x32,abc,136,19)
				//fmt.Printf("%x\n",com_cmd)
				all_machines[inRoomMAC].socket.Write(com_cmd)
			}else if strCmd == "exit_room"{
				//如果curplayer是它 则置nil
				if all_machines[inRoomMAC].current_player == conn{
					t1 := all_machines[inRoomMAC]
					t1.current_player = nil
					all_machines[inRoomMAC] = t1
				}

				inRoomMAC=""
			}
	     }
	 }

	/*变参数的用法
	func sum(nums ...int) {
    fmt.Print(nums, " ")
    total := 0
    for _, num := range nums {
        total += num
    }
    fmt.Println(total)
	}
	*/
}


//====
func Echo(ws *websocket.Conn) {
    var err error
    var inRoomMAC string 
    for {
        /*var reply string
        //websocket接受信息
        if err = websocket.Message.Receive(ws, &reply); err != nil {
            fmt.Println("receive failed:", err)
            break
        }*/

        var reply string
        //websocket接受信息
        if err = websocket.Message.Receive(ws, &reply); err != nil {
            fmt.Println("receive failed:", err)
            break
        }

 		data2 := []byte(reply)
		/*for i:=0;i<len(data2);i++{
			fmt.Printf("%02x", data2[i])
	    }
	    fmt.Println();

        string2 := string(data2[3:])
 		fmt.Println("string is:" + string2)
	    */

        fmt.Println("reveived from clien1t:" + reply)

        var dat map[string]interface{}
		json.Unmarshal(data2[:], &dat)
		strCmd := dat["cmd"]

		if strCmd=="req_roomlist"{
			roomList := make([]string, len(all_machines))
			indexI := 0

				type RoomRet struct{
				Cmd string `json:"cmd"`
				Roomlist []string `json:"rooms"`
			}

			for k, _ := range all_machines {
	   			roomList[indexI] = k
	   			indexI++
			}

			var roList = RoomRet{Cmd:"reply_roomlist", Roomlist:roomList}
			b2,err2 := json.Marshal(roList)                                                             
			if err2 != nil{                                                                    
				fmt.Println("err 2", err)                                                       
			}                                                                                    
			fmt.Println(string(b2))

			if err = websocket.Message.Send(ws, string(b2)); err != nil {
            	fmt.Println("send failed:", err)
           		break
        	}

		}else if strCmd=="enter_room"{
				strMAC:=dat["mac"]
				inRoomMAC = strMAC.(string)
		}else if strCmd == "start_game"{
				//当前玩家
			t1 := all_machines[inRoomMAC]
			t1.current_player = ws
			all_machines[inRoomMAC] = t1

			//下发指令到娃娃机
			com_cmd := make_com(0x31,60,0,0,0,0,0,0,0,0,0,0)
			all_machines[inRoomMAC].socket.Write(com_cmd)
			}else if strCmd == "operation"{
				abc := byte(dat["type"].(float64))
	
				//下发指令到娃娃机
				com_cmd := make_com(0x32,abc,136,19)
				//fmt.Printf("%x\n",com_cmd)
				all_machines[inRoomMAC].socket.Write(com_cmd)
			}else if strCmd == "exit_room"{
				//如果curplayer是它 则置nil
				if all_machines[inRoomMAC].current_player == ws{
					t1 := all_machines[inRoomMAC]
					t1.current_player = nil
					all_machines[inRoomMAC] = t1
				}

				inRoomMAC=""
			}

        //fmt.Println("send to client:" + msg)
        //这里是发送消息
        //if err = websocket.Message.Send(ws, msg); err != nil {
        //    fmt.Println("send failed:", err)
        //    break
       // }
    }
}
