/*
 * nt_base_code_define.cs
 * nt_base_code_define
 * 
 * Github: https://github.com/daniulive/SmarterStreaming
 * 
 * Created by DaniuLive on 2015/05/08.
 * Copyright © 2014~2017 DaniuLive. All rights reserved.
 */

using System;
using System.Runtime.InteropServices;

namespace NT
{
    public static class NTBaseCodeDefine
    {

        /* 基本错误码定义 */
        public const UInt32 NT_ERC_OK = 0x00000000;

        /* 错误码
         * 
         * {XX,XX,XX,XX}, 所有描述从左到右，最左边为第一个字节，以此类推。
         * {XX}, 对于一个字节来说，左边X是高四位，右边X为低四位
         * 错误码模块定义：第一个字节的低四位和第二个字节, 具体错误原因占后两个字节
         * 对于通用错误，｛90, 00, XX, XX},也就是说通用错误模块ID为 {0, 00}
         */
        public const UInt32 NT_ERC_BASE = 0x90000000;

        /* 下面的定义是通用错误 */
        public const UInt32 NT_ERC_FAILED = 0x90000001;
        public const UInt32 NT_ERC_NULL_POINTER = 0x90000002; // 空指针错误

        /* SmartLog 错误码 */
        public const UInt32 NT_ERC_SMART_LOG = 0x90010000;

        /* SmartClientSDK 错误码 */
        public const UInt32 NT_ERC_SMART_CLIENT_SDK = 0x90020000;

        /* SmartPlayerSDK 错误码 */
        public const UInt32 NT_ERC_SMART_PLAYER_SDK = 0x90030000;

        /* SmartRenderSDK 错误码*/
        public const UInt32 NT_ERC_SMART_RENDER_SDK = 0x90040000;

        /* SmartPublisherSDK 错误码*/
        public const UInt32 NT_ERC_SMART_PUBLISHER_SDK = 0x90050000;

        /* 其他模块请继续定义 */

        /*参数ID, 主要为SetParam和GetParam接口服务 */
        public const UInt32 NT_PARAM_ID_BASE = 0x00000000;

        /*参数ID，模块划分规则，使用前两个字节作区分模块id, 公用参数，前两个字节为0*/
        public const UInt32 NT_PARAM_ID_COMMON_BASE = (NT_PARAM_ID_BASE);

        /* SmartLog 参数ID */
        public const UInt32 NT_PARAM_ID_SMART_LOG = 0x00010000;

        /*SmartClientSDK 参数ID*/
        public const UInt32 NT_PARAM_ID_SMART_CLIENT_SDK = 0x00020000;

        /*SmartPlayerSDK 参数ID*/
        public const UInt32 NT_PARAM_ID_SMART_PLAYER_SDK = 0x00030000;

        /*SmartRenderSDK 参数ID*/
        public const UInt32 NT_PARAM_ID_SMART_RENDER_SDK = 0x00040000;

        /* SmartPublisherSDK 参数ID */
        public const UInt32 NT_PARAM_ID_SMART_PUBLISHER_SDK = 0x00050000;

        /*事件ID*/
        /*一些通用的事件ID*/
        public const UInt32 NT_EVENT_ID_COMMON_BASE	= 0x00000000;

        /*SmartPlayerSDK 事件ID*/
        public const UInt32 NT_EVENT_ID_SMART_PLAYER_SDK = 0x01000000;

        /*SmartPublisherSDK 事件ID*/
        public const UInt32 NT_EVENT_ID_SMART_PUBLISHER_SDK = 0x02000000;

    }
}