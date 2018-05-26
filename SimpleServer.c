/**
 * Tcp Server program, It is a simple example only.
 * zhengsh 200520602061 2
 * when client connect to server, send a welcome message and timestamp in server.
 */

#include <stdio.h> 
#include <sys/socket.h> 
#include <unistd.h> 
#include <sys/types.h> 
#include <netinet/in.h> 
#include <stdlib.h> 
#include <time.h> 
#include <string.h>
#include <thread>
#include <string>
#include <map>
#include <netinet/tcp.h>
using namespace std;

#pragma warning (disable:4996)

typedef void(*ext)();

int PLAYER_SOCKET = -1;
int ROOM_SOCKET = -1;
char g_wawaji_mac[13];//mac of that doll machine

bool stopRunning = false;

//for debug print time only
void PrintTime() {
	time_t t;
	struct tm * lt;
	time(&t);//
	lt = localtime(&t);
	printf("[%d:%d:%d]",  lt->tm_hour, lt->tm_min, lt->tm_sec);
}

//保证消息接收完整性
bool recv_unti(int socketFD, char* buffer, int len) {
	int total_recved = 0;

	int recv_len = recv(socketFD, buffer + total_recved, len- total_recved, 0);
	if (recv_len <= 0) {
		printf("recv_len<=0:%d\r\n", recv_len);
		return false;
	} 
	total_recved += recv_len;

	while (total_recved < len) {
		recv_len = recv(socketFD, buffer + total_recved, len - total_recved, 0);
		if (recv_len <= 0){
			printf("recv_len1<=0");
			printf("recv_len<=0:%d\r\n", recv_len&0xffff);
			return false;
		}
		total_recved += recv_len;
	}

	return true;
}

bool check_com_data(char* data, int len) {
	if (len < 6) return false;

	//calc the sum
	int check_total = 0;
	for (int i = 0; i < len; i++) {
		if ((i >= 6) && (i < len - 1))
			check_total += (data[i] & 0xff);
	}

	if (data[0] != (unsigned char)((~data[3]) & 0xff)
		&& data[1] != (unsigned char)((~data[4]) & 0xff)
		&& data[2] != (unsigned char)((~data[5]) & 0xff))
		return false;

	//check sum
	if (check_total % 100 != (data[len - 1] & 0xff)) {
		return false;
	}

	return true;
}

void player_recv(){
	while (stopRunning == false){
		char bu_len[7] = { 0 };
		if(recv_unti(PLAYER_SOCKET, bu_len, 7) == false)
			break;

		int data_len = (unsigned char)bu_len[6];

		if ((bu_len[0] & 0xff) != 0xfe)
			break;

		char* pData = new char[data_len];
		memset(pData, 0, data_len);
		memcpy(pData, bu_len, 7);
		bool is_ok = recv_unti(PLAYER_SOCKET, pData + 7, data_len - 7);
		if (is_ok == false) {
			delete[]pData;
			break;
		}

		PrintTime();
		printf("player_data:");
		for (int i = 0; i < data_len; i++) {
			printf("%02X ", pData[i]&0xff);
		}
		printf("\r\n");

		if (check_com_data(pData, data_len) == false) {
			printf("data check error.\r\n");
			delete[]pData;
			continue;
		}

		if( (pData[7]&0xff) == 0x1){//20180526. i heard some people is using this server.so, i add support for the SimpleClient. Cause the SimpleClient-C.apk is out of date, and no sourcecode.
			//request room list.
			printf("player request room list ");
			char room_list[128]= {0};
			if( strcmp(g_wawaji_mac, "") != 0){
				sprintf(room_list, "[\"%s\"]", g_wawaji_mac);// only one wawaji is valid to this server. as you know .as simple as possbile.
				printf("reply roomlist %s\n", room_list);
			}else{
				printf("no room. reply empty.\n");
				sprintf(room_list, "[]");
			}

			int pack_len = 9 + strlen(room_list);
			unsigned char* msg_content = new unsigned char[pack_len];
			msg_content[0] = 0xfe;
			msg_content[1] =  0;
			msg_content[2] =  0;
			msg_content[3] = ~msg_content[0];
			msg_content[4] = ~msg_content[1];
			msg_content[5] = ~msg_content[2];
			msg_content[6] = pack_len;
			msg_content[7] = 0x1;

			memcpy(msg_content+8, room_list, strlen(room_list));
			int total_c = 0;
			for(int ikk = 6; ikk< pack_len-1; ikk++)
			{
				total_c += (msg_content[ikk] & 0xff);
			}
			msg_content[pack_len-1] = (total_c % 100);

			for( int k = 0; k< pack_len; k++)
				printf("%02X ", msg_content[k]);

			printf("\n");

			send(PLAYER_SOCKET, msg_content, pack_len, 0);

			delete []msg_content;
		}else if ( (pData[7]&0xff) == 0x31){// new game recv. you should decide whether to grasp or not. here ,i simply translate to the dool machine.
			//玩家点了开局按钮
			PrintTime();
			printf("cmd:request new game.tranlating to room ..\r\n");

			if (ROOM_SOCKET != -1){ //有娃娃机 直接转发到娃娃机
				send(ROOM_SOCKET, pData, data_len, 0);//send to the doll machine.let it begin!
			}

			send(PLAYER_SOCKET, pData, data_len, 0);//send back to the player to enable the gui button. 
			//In fact you should wait the reply from the doll machine . Translate it's replay to player. You should check to make sure the doll machine is ok.
		}else if (ROOM_SOCKET != -1){//任何消息都直接转发到娃娃机.但是实际场景中要处理登录 获取房间列表等等一堆东西。这些东西是不能转发给娃娃机。而需要你自己处理
			PrintTime();
			printf(" tranlating to room ..\r\n");

			send(ROOM_SOCKET, pData, data_len, 0);//operation from the player(APP). translate to the doll machine directly.
			//well in real scene ,the player cmd is much more than operation cmd. you should handle it by yourself. Not in this simple server!
		}

		 delete[]pData;
	}

	printf("\nplayer close.\n");
	close(PLAYER_SOCKET); PLAYER_SOCKET = -1;
}

//处理娃娃机过来的消息
void room_recv(){
	while (stopRunning == false){
		char bu_len[7] = { 0 };
		if (recv_unti(ROOM_SOCKET, bu_len, 7) == false) {
			printf("recv head error.");
			break;
		}


		printf("room_head:");
		for (int i = 0; i < 7; i++){
			printf("%02X ", bu_len[i]&0xff);
		}

		printf("\r\n");


        if((bu_len[0] &0xff) != 0xfe)
            break;

		int data_len = (unsigned char)bu_len[6];

		char* pData = new char[data_len];
		memset(pData, 0, data_len);
		memcpy(pData, bu_len, 7);


		bool is_ok = recv_unti(ROOM_SOCKET, pData + 7, data_len - 7);
		if (is_ok == false){
			delete[]pData;
			break;
		}

		PrintTime();
		printf("room_data:");
		for (int i = 0; i < data_len; i++){
			printf("%02X ", pData[i]&0xff);
		}

		printf("\r\n");
		printf("cmd:%02X\r\n", (pData[7]&0xff)) ;

		if (check_com_data(pData, data_len) == false){
			printf("data check error.Room Close\r\n");
			delete[]pData;pData = NULL;
			break;
		}

		//如果是心跳and 0x92。直接原样返回
		if ((pData[7]&0xff) == 0x35)//heartbeat msg from the doll machine. you should flag this server as 'live'. More than 30s is dead...
		{
			memcpy(g_wawaji_mac,pData + 8, 12);
			printf("mac recv is:%s\r\n", g_wawaji_mac);

			send(ROOM_SOCKET, pData, data_len, 0);
		}
		else if((pData[7]&0xff) == 0x92)
		{
			send(ROOM_SOCKET, pData, data_len, 0);//reply the same to stop fire read timeout.
		}
		else if (PLAYER_SOCKET != -1)//否则如果有玩家，就把任何消息转发给玩家
		{//实际应用中，你还得处理故障和游戏结束后的回调消息。如果玩家抓到娃娃..等等消息。所以不能只是无脑转发给玩家
			//此例子只是演示基本核心逻辑
			printf(" tranlating to client.\n");
			send(PLAYER_SOCKET, pData, data_len, 0);
		}

		delete[]pData;
	}

	memset( g_wawaji_mac, 0, 13);
	PrintTime();
	printf("\nRoom close.\r\n");
	close(ROOM_SOCKET);
	ROOM_SOCKET = -1;
}

int g_doll_listen = -1;
int g_player_listen = -1;

void Comm(ext func, int Port){
	int  servfd, clifd;
	struct  sockaddr_in servaddr, cliaddr;

	if ((servfd = socket(AF_INET, SOCK_STREAM, 0)) < 0){
		PrintTime();
		printf("create socket error!\n ");
		exit(1);
	}

	servaddr.sin_family = AF_INET;
	servaddr.sin_port = htons(Port);
	servaddr.sin_addr.s_addr = htons(INADDR_ANY);

	int btrue = 1;
	setsockopt(servfd,SOL_SOCKET,SO_REUSEADDR,&btrue,sizeof(int));

	if (bind(servfd, (struct  sockaddr *) & servaddr, sizeof(servaddr)) < 0){
		PrintTime();
		printf("bind to port %d failure!\n", Port);
		exit(1);
	}

	if (listen(servfd, 10) < 0){
		PrintTime();
		printf("call listen failure!\n");
		exit(1);
	}

	if (Port == 7770) g_doll_listen = servfd;
	else if (Port == 7771) g_player_listen = servfd;

	PrintTime();
	printf("start listen at port: %d !\n", Port);
	while (stopRunning == false)
	{ // server loop will nerver exit unless any body kill the process 

		long  timestamp;
		socklen_t length = sizeof(cliaddr);
		clifd = accept(servfd, (struct  sockaddr *) & cliaddr, &length);
		if (clifd < 0){
			PrintTime();
			printf(" error comes when call accept!\n");
			break;
		}

		if (Port == 7771) //another client connected. by we only support one client in this simple server. 
			//so we close last player.You should write your own server code to support many player...
		{
			if (PLAYER_SOCKET != -1)
			{
				close(PLAYER_SOCKET);
				PLAYER_SOCKET = -1;
			}

			PLAYER_SOCKET = clifd;
		}
		else if (Port == 7770)//Same. This simple server is functionally demo. So ,it's not support many doll to make logic simple ,close the old doll machine.
		{//write you own server code to support many doll machine.
			if (ROOM_SOCKET != -1)
			{
				PrintTime();
				printf("Another room online Close Old Room.\r\n");
				close(ROOM_SOCKET);
				ROOM_SOCKET = -1;
			}

			printf("room connect.\r\n");
			ROOM_SOCKET = clifd;
		}

		thread th_recv(func);
		th_recv.detach();

	} // exit 

	close(servfd);
	printf("listen thread exit\r\n");
}

//#define PAUSE printf("Press Enter key to continue..."); fgetc(stdin);

int main(int argc, char** argv)
{
	thread th_listen_room(Comm, room_recv, 7770);
	th_listen_room.detach();

	thread th_listen_player(Comm, player_recv, 7771);
	th_listen_player.detach();

	while (1){
		char chIn[20] = { 0 };

		fgets(chIn, 20, stdin);
		printf("input is:%s\n", chIn);
		if (strstr(chIn, "exit") != NULL){
			if (ROOM_SOCKET != -1) {
				printf("ROOM_SOCKET:%d close.\n", ROOM_SOCKET);
				close(ROOM_SOCKET); ROOM_SOCKET = -1;
			}

			if (PLAYER_SOCKET != -1) {
				printf("PLAYER_SOCKET :%d close.\n", PLAYER_SOCKET);
				close(PLAYER_SOCKET); PLAYER_SOCKET = -1;
			}

			printf("g_doll_listen:%d close.\n", g_doll_listen);
			close(g_doll_listen);g_doll_listen = -1;

			printf("g_player_listen:%d close.\n", g_player_listen);
			close(g_player_listen);g_player_listen = -1;
			stopRunning = true;
			printf(" stop running now");
			//PAUSE;
			break;
		}
	}

	return   0;
}
