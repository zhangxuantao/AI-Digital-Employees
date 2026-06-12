## 一、重点说明
_**常用业务对象**_中各个业务功能中 `a`、`b`项描述如下:  
_**发送**_：特指 websocket 消息发送  
_**接收**_：特指 websocket 消息接收  
业务执行顺序为`a > b`  
`a.发送`后大部分业务会收到以 `resp.`为前缀的`b.接收`响应消息  
`a.接收`的消息为单向类型，例如客户消息、系统消息  
关于_**客户列表**_，服务端是通过遍历客户列表，逐一推送 `subscriber.open`到前端的，前端缓存客户，生成客户列表  
关于_**消息列表**_，客户新消息会由前端缓存，写入对应客户的消息列表，点击客户列表中的客户头像，在打开的客户会话窗口中拉取消息列表；如果消息列表为空，自动调用接口从服务端拉取历史消息列表

## 二、通用消息对象
数据对象最外层通用字段，具体业务对象包含在`data`中。后续业务对象介绍不在介绍此通用字段

### 参数列表
| _**参数**_ | _**类型**_ | _**校验**_ | _**描述**_ |
| :--- | :--- | :--- | :--- |
| type | string | 必填 | 消息业务类型 |
| data | object | 必填 | 消息数据体 |
| id | int | 可选 | socket消息id |
| code | int、string | 可选 | 消息码，默认为0,大于0为错误码 |
| … | object | 可选 | 外层其他字段根据具体业务类型定义 |


### JSON格式
```plain
{
    "type": "ping.request",
    "data": {
        "time": 1607490338748
    },
    "id": "",
    "code": 0,
}
```

## 三、常用业务消息对象
### 1. 心跳响应
由于在线业务对网络响应要求较高，且websocket在网络波动的情况下并不一定会断开连接，所以为了检测伪掉线情况，由客户端`定时`向服务端主动发起请求，服务端进行响应。未及时响应将会被认为已经掉线，从而进行会引起客户端`重连`尝试。

#### a. 发送
消息类型：`ping.request`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| id | string | socket消息id |
| type | string | 消息业务类型 |
| - data | object | 消息体 |
| time | number | 时间戳 |


##### JSON格式
```plain
{
     "type": "ping.request",
     "data": {
        "time": 1607490338748
     }
}
```

#### b. 接收
消息类型：`pong.response`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| code | number | 无 |
| - data | object | 无 |
| src_emp_code | string | 源客服工号 |
| time | number | 时间戳 |
| id | string | 无 |
| type | string | 消息业务类型 |


##### JSON格式
```plain
{
    "code": 0,
    "data": {
        "src_emp_code": "00397589",
        "time": 1607415824
    },
    "id": "1607415824894",
    "type": "pong.response"
}
```

### 4. 客服状态通知
服务端定时通知客服的最新状态，并更新页面状态按钮显示效果

#### a. 接收
消息类型：`employee.status`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| - data | object | 无 |
| status | string | 状态 |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "data": {
        "status": "busy"
    },
    "id": "6742268555460345856",
    "type": "employee.status"
}
```

### 5. 客服状态切换
客服进行切换状态，在线online、忙碌busy、离线offline

#### a. 发送
消息类型：`employee.status`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| id | string | 无 |
| type | string | 无 |
| - data | object | 无 |
| status | string | 新状态 |
| statusbefore | string | 旧状态 |


##### JSON格式
```plain
{
    "id": "",
    "type": "employee.status",
    "data": {
        "status": "busy",
        "statusbefore": "offline"
    }
}
```

#### b. 接收
消息类型：`resp.employee.status`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| code | number | socket消息编码 |
| - data | object | 无 |
| level | number | 无 |
| statusbefore | string | 前一个状态 |
| status | string | 当前状态 |
| id | string | 无 |
| message | string | socket消息内容 |
| type | string | 无 |


##### JSON格式
```plain
{
    "code": 0,
    "data": {
        "level": 0,
        "statusbefore": "busy",
        "status": "busy"
    },
    "id": "6742268681197191168",
    "message": "ok",
    "type": "resp.employee.status"
}
```

### 6. 消息接收-客户
客户来消息，需要客服进行回复

#### a. 接收
消息类型：`message.request`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| - data | object | 无 |
| app_code | string | 渠道代码 |
| category | string | 消息业务类型：autreq自助请求、autres自助回复、watreq排队请求、watres排队回复、manreq人工请求、manres人工回复、sys_ssncls系统关闭、surreq客户评价、ssn_timeout系统回复 |
| create_at | number | 创建时间，时间戳 |
| ent_code | string | 企业代码 |
| - - msg_body | object | 消息体 |
| content | string | 文本内容 |
| msg_id | number | 消息id |
| msg_type | string | 消息类型 |
| openid | string | 渠道客户id |
| platform | string | 渠道名称 |
| send_at | number | 发送时间，时间戳 |
| sub_id | number | 客户id |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "data": {
        "app_code": "site_yto",
        "category": "manreq",
        "create_at": 1585014408,
        "ent_code": "yto",
        "msg_body": {
            "content": "嗯"
        },
        "msg_id": 267548455,
        "msg_type": "text",
        "openid": "yipkhezsgf7n9o4lwdp3lh3jc0nf9vvhg3zcbics",
        "platform": "site",
        "send_at": 1585014408,
        "sub_id": 132922995
    },
    "id": "6650662839587438592",
    "type": "message.request"
}
```

### 7. 消息发送-客服
客户来消息之后，客服需要进行回复

#### a. 发送
消息类型：`message.response`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| id | string | 无 |
| temp_id | string | 时间戳 |
| type | string | 无 |
| data | object | 无 |
| - sub_id | number | 客户id |
| - srv_ssn_id | number | 服务会话id |
| - msg_type | string | 消息类型 |
| - msg_body | object | 消息体 |
| - - content | string | 文本内容 |
| - assist_id | number | 辅助话术id，当使用辅助话术窗口的发送按钮后使用 |
| - reply_index | number | 辅助话术索引，当使用辅助话术窗口的发送按钮后使用 |
| - adopt | bool | 是否采纳(辅助话术与客服回复一致)，当使用辅助话术窗口的发送按钮后使用 |


##### JSON格式
```plain
# 发送文本
{
    "id": "",
    "temp_id": "1607490338386",
    "type": "message.response",
    "data": {
        "sub_id": 132922861,
        "srv_ssn_id": 18794023,
        "msg_type": "text",
        "msg_body": {
            "content": "1"
        },
        "assist_id": 111,
        "reply_index": 0,
        "adopt": true
    }
}
# 发送图片
{
    "id": "",
    "type": "message.response",
    "data": {
        "category": "manres",
        "create_at": 1690867213,
        "send_at": 1690867213,
        "msg_id": 268150769,
        "msg_body": {
            "media_id": 525,
            "pic_url": "group2/M00/00/19/CoIk6WTIlgyIFm5vAAF417_2etAAAAAjwEA5DMAAXj5468.jpg",
        },
        "msg_type": "image",
        "sub_id": 132947775,
        "sub_ssn_id": 20192509,
        "srv_ssn_id": 18821479,
        "emp_id": 10135038
    }
}
```

#### b. 接收
消息类型：`resp.response`，系统收到消息的自动响应

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| code | number | socket消息编码 |
| - data | object | 无 |
| temp_id | string | 时间戳 |
| id | string | 无 |
| message | string | socket消息内容 |
| type | string | 无 |


##### JSON格式
```plain
{
    "code": 0,
    "data": {},
    "temp_id": "1607490338386",
    "id": "6742303156207616000",
    "message": "ok",
    "type": "resp.response"
}
```

#### c. 接收
消息类型：`resp.message.response`，业务处理响应

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| code | number | socket消息编码,默认0，大于0表示有错误，具体错误内容查看message |
| - data | object | 无 |
| srv_ssn_id | number | 服务会话id |
| sub_id | number | 客户id |
| create_at | number | 创建时间 |
| msg_id | number | 消息id |
| temp_id | string | 时间戳 |
| id | string | 无 |
| message | string | socket消息内容，默认为ok，否则为错误消息 |
| type | string | 无 |


##### JSON格式
```plain
{
    "code": 0,
    "data": {
        "srv_ssn_id": 18794019,
        "sub_id": 132922861,
        "create_at": 1607483273,
        "msg_id": 267561471
    },
    "temp_id": "1607483273139",
    "id": "6742273522610995200",
    "message": "ok",
    "type": "resp.message.response"
}
# 错误返回
{
    "code": 30008,
    "data": {
        "srv_ssn_id": 18794019,
        "sub_id": 132922861,
        "create_at": 1607483273,
        "msg_id": 267561471
    },
    "temp_id": "1607483273139",
    "id": "6742273522610995200",
    "message": "亲爱的客服MM，禁止向客户发送不文明/不礼貌用语，系统已拦截你发送的非法词，情稳定自己的情绪，您所发送的非法词'呵呵'已通知您的负责人。",
    "type": "resp.message.response"
}
```

### 8. 新客户
有新客户进入，由前端生成客户列表

#### a. 接收
消息类型：`subscriber.open`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| - data | object | 无 |
| country | string | 国家 |
| subscribe_at | number | 关注事件，时间戳 |
| srv_ssn_id | number | 服务会话id |
| gender | string | 性别，unset、male、female |
| city | string | 城市 |
| ent_time | number | 进入时间，时间戳 |
| openid | string | 渠道客户id |
| sub_id | number | 客户id |
| sub_ssn_id | number | 客户会话id |
| telephone | string | 电话 |
| platform | string | 渠道 |
| profile_img | string | 头像图片url |
| province | string | 省份 |
| sub_type | number | 客户类型 |
| nickname | string | 昵称 |
| platform_name | string | 渠道中文名 |
| alias | string | 别称 |
| email | string | 邮箱 |
| ent_code | string | 企业代码 |
| app_code | string | 渠道代码 |
| session_type | number | 会话类型，1：三方会话 |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "data": {
        "country": "",
        "subscribe_at": 1607483031,
        "srv_ssn_id": 18794019,
        "gender": "unset",
        "city": "",
        "ent_time": 1607483073,
        "openid": "jar-9akcyfbrhgb18rltjfm6aqahos1c4jrn_c-n",
        "sub_id": 132922861,
        "sub_ssn_id": 20163789,
        "telephone": "",
        "platform": "site",
        "profile_img": "",
        "province": "",
        "sub_type": 1,
        "nickname": "客户_132922861",
        "platform_name": "官网在线",
        "alias": "",
        "email": "",
        "ent_code": "yto",
        "app_code": "site_yto",
        "session_type": 0
    },
    "id": "6742272687810281472",
    "type": "subscriber.open"
}
```

### 9. 客户关闭-客户
客户已离开，需要关闭会话窗口，并从客户列表移除(具体参考新需求)

#### a. 接收
消息类型：`subscriebr.close`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| - data | object | 无 |
| srv_ssn_id | number | 服务会话id |
| openid | string | 渠道客户id |
| stop_mode | string | 关闭方式 |
| sub_id | number | 客户id |
| sub_ssn_id | number | 客户会话id |
| ent_code | string | 企业代码 |
| platform | string | 渠道 |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "data": {
        "srv_ssn_id": 18794025,
        "openid": "6e91d944-57cf-497e-82d5-3868767a342b",
        "stop_mode": "sub_survey",
        "sub_id": 132922863,
        "sub_ssn_id": 20163791,
        "ent_code": "yto",
        "platform": "wechat"
    },
    "id": "6742305388693028864",
    "type": "subscriebr.close"
}
```

### 10. 客户关闭-客服
客服主动发起客户关闭，如满意度调查、强制关闭等

#### a. 发送
消息类型：`session.close`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| id | string | 无 |
| temp_id | string | 客户id |
| type | string | 无 |
| - data | object | 无 |
| sub_id | number | 客户id |
| srv_ssn_id | number | 服务会话id |
| stop_mode | string | 关闭模式 |


##### JSON格式
```plain
{
    "id": "",
    "temp_id": "132922861",
    "type": "session.close",
    "data": {
        "sub_id": 132922861,
        "srv_ssn_id": 18794023,
        "stop_mode": "sub_force"
    }
}
```

#### b. 接收
消息类型：`resp.session.close`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| code | number | socket消息编码，默认0，大于0表示有错误 |
| - data | object | 无 |
| srv_ssn_id | number | 服务会话id |
| stop_mode | string | 关闭模式 |
| sub_id | number | 客户id |
| update_at | number | 更新时间 |
| stop_at | number | 关闭时间 |
| temp_id | string | 客户id |
| id | string | 无 |
| message | string | socket消息内容 |
| type | string | 无 |


##### JSON格式
```plain
{
    "code": 0,
    "data": {
        "srv_ssn_id": 18794023,
        "stop_mode": "sub_force",
        "sub_id": 132922861,
        "update_at": 1607490339184,
        "stop_at": 1607490339000
    },
    "temp_id": "132922861",
    "id": "6742303159613390848",
    "message": "ok",
    "type": "resp.session.close"
}
```

### 11. 客户超时提醒
当客户3分钟未说话，在客户列表对应的客户图标上进行标记，(具体参考新需求)

#### a. 接收
消息类型：`subscriber.timeout`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| - data | object | 无 |
| srv_ssn_id | number | 服务会话id |
| openid | string | 渠道客户id |
| sub_id | number | 客户id |
| sub_ssn_id | number | 客户会话id |
| ent_code | string | 企业代码 |
| platform | string | 渠道 |
| app_code | string | 渠道代码 |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "data": {
        "srv_ssn_id": 18794019,
        "openid": "jar-9akcyfbrhgb18rltjfm6aqahos1c4jrn_c-n",
        "sub_id": 132922861,
        "sub_ssn_id": 0,
        "ent_code": "yto",
        "platform": "site",
        "app_code": "site_yto"
    },
    "id": "6742274289178771456",
    "type": "subscriber.timeout"
}
```

### 13. 消息发送失败提示
socket消息发送到服务端后，业务执行失败的错误提示

#### a. 发送
消息类型：`message.error`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| - data | object | 无 |
| msg_err_id | number | 无 |
| srv_ssn_id | number | 服务会话id |
| err_msg | string | socket错误消息内容 |
| openid | string | 渠道客户id |
| sub_id | number | 客户id |
| count | number | 无 |
| err_code | string | socket错误消息编码 |
| msg_id | number | 消息id |
| ent_code | string | 企业代码 |
| platform | string | 渠道 |
| app_code | string | 渠道代码 |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "data": {
        "msg_err_id": 0,
        "srv_ssn_id": 0,
        "err_msg": "推送客服消息到官网在线失败(客户离线)",
        "openid": "jar-9akcyfbrhgb18rltjfm6aqahos1c4jrn_c-n",
        "sub_id": 132922861,
        "count": 0,
        "err_code": "50000",
        "msg_id": 267561475,
        "ent_code": "yto",
        "platform": "site",
        "app_code": "site_yto"
    },
    "id": "6742303159382704128",
    "type": "message.error"
}
```

### 14. socket客户端挤下线
同一个客服只允许开启一个会话窗口，否则会进行挤下线(老功能)

#### a. 接收
消息类型：`employee.kick`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| code | number | 无 |
| - data | object | 无 |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "code": 0,
    "data": {},
    "id": "1607415824894",
    "type": "employee.kick"
}
```

#### b. 接收
消息类型：`wdgj.kick`,来自新Portal的挤下线

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| code | number | 无 |
| - data | object | 无 |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "code": 0,
    "data": {},
    "id": "1607415824894",
    "type": "wdgj.kick"
}
```

### 15. 客服当日接待统计
会话窗口左下角统计栏显示客服的接待量等数据

#### a. 接收
消息类型：`employee.report`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| code | number | 无 |
| - data | object | 无 |
| avg_rsp_duration | number | 平均响应时长，单位：秒 |
| avg_score | number | 质检平均分 |
| emp_code | string | 客服代码 |
| emp_name | string | 客服姓名 |
| sat_degree | string | 满意度，无数据显示：未评价或暂无 |
| sum_valid | number | 接待量 |
| id | number | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "code": 0,
    "data": {
        "avg_rsp_duration": 15,
        "avg_score": 98,
        "emp_code": "00397589",
        "emp_name": "侯荣勤",
        "sat_degree": "78.6%",
        "sum_valid": 120
    },
    "id": 1607482112924,
    "type": "employee.report"
}
```

### 17. 发起会话转移请求-发起方
客服可以将客户转给其他人

#### a. 发送
消息类型：`transfer.request`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| id | string | 无 |
| type | string | 无 |
| - data | object | 无 |
| sub_id | number | 客户id |
| srv_ssn_id | number | 服务会话id |
| dst_emp_code | string | 目标客服代码 |
| note | string | 备注 |


##### JSON格式
```plain
{
    "id": "",
    "type": "transfer.request",
    "data": {
        "sub_id": 111,
        "srv_ssn_id": 111,
        "dst_emp_code": "00398671",
        "note": ""
    }
}
```

#### b. 接收
消息类型：`resp.transfer.request`，系统响应

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| code | number | socket消息代码 |
| - data | object | 无 |
| srv_ssn_id | number | 服务会话id |
| sub_id | number | 客户id |
| tran_ssn_id | number | 会话转移id |
| dst_emp_code | string | 目标客服代码 |
| id | string | 无 |
| message | string | socket消息内容 |
| type | string | 无 |


##### JSON格式
```plain
{
    "code": 0,
    "data": {
        "srv_ssn_id": 18794023,
        "sub_id": 132922861,
        "tran_ssn_id": 0,
        "dst_emp_code": "00069119"
    },
    "id": "6742285088362332160",
    "message": "ok",
    "type": "resp.transfer.request"
}
```

### 18. 收到会话转移通知-接入方
其他客服向你发送会话转移，弹窗提示，可选择接受或者拒绝；60秒未选择`自动接受`。

#### a. 发送
消息类型：`transfer.request`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| id | string | 无 |
| type | string | 无 |
| - data | object | 无 |
| sub_id | number | 客户id |
| srv_ssn_id | number | 服务会话id |
| dst_emp_code | string | 目标客服代码 |
| note | string | 备注 |


##### JSON格式
```plain
{
    "id": "",
    "type": "transfer.request",
    "data": {
        "sub_id": 132922861,
        "srv_ssn_id": 18794019,
        "dst_emp_code": "00069119",
        "note": "帮忙接个客户"
    }
}
```

### 19. 会话转移接受-接入方
有会话转移接入，选择接受

#### a. 发送
消息类型：`transfer.accept`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| id | string | 无 |
| type | string | 无 |
| - data | object | 无 |
| tran_ssn_id | number | 会话转移id |
| srv_ssn_id | number | 服务会话id |
| sub_id | number | 客户id |


##### JSON格式
```plain
{
    "id": "",
    "type": "transfer.accept",
    "data": {
        "tran_ssn_id": 123,
        "srv_ssn_id": 18794019,
        "sub_id": 132922861
    }
}
```

#### b. 接收
消息类型：`resp.transfer.accept`，系统自动响应

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| code | number | socket消息编码 |
| - data | object | 无 |
| srv_ssn_id | number | 服务会话id |
| sub_id | number | 客户id |
| tran_ssn_id | number | 会话转移id |
| id | string | 无 |
| message | string | socket消息内容 |
| type | string | 无 |


##### JSON格式
```plain
{
    "code": 0,
    "data": {
        "srv_ssn_id": 18794023,
        "sub_id": 132922861,
        "tran_ssn_id": 117
    },
    "id": "6742284026758823936",
    "message": "ok",
    "type": "resp.transfer.accept"
}
```

### 20. 会话转移强制接受-接入方
有会话转移接入，60秒内未选择自动接受

#### a. 发送
消息类型：`transfer.force`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| - data | object | 无 |
| sub_nickname | string | 客户昵称 |
| srv_ssn_id | number | 服务会话id |
| openid | string | 渠道客户id |
| sub_id | number | 客户id |
| platform_name | string | 渠道中文名称 |
| src_emp_code | string | 发起源客服工号 |
| tran_ssn_id | number | 会话转移id |
| dst_emp_code | string | 目标客服工号 |
| ent_code | string | 企业代码 |
| platform | string | 渠道 |
| app_code | string | 渠道代码 |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "data": {
        "sub_nickname": "客户_132922861",
        "srv_ssn_id": 18794021,
        "openid": "jar-9akcyfbrhgb18rltjfm6aqahos1c4jrn_c-n",
        "sub_id": 132922861,
        "platform_name": "官网在线",
        "src_emp_code": "00069119",
        "tran_ssn_id": 117,
        "dst_emp_code": "00397589",
        "ent_code": "yto",
        "platform": "site",
        "app_code": "site_yto"
    },
    "id": "6742284026188398592",
    "type": "transfer.force"
}
```

#### b. 接收
消息类型：`resp.transfer.accept`，系统自动响应

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| code | number | socket消息编码 |
| - data | object | 无 |
| srv_ssn_id | number | 服务会话id |
| sub_id | number | 客户id |
| tran_ssn_id | number | 会话转移id |
| id | string | 无 |
| message | string | socket消息内容 |
| type | string | 无 |


##### JSON格式
```plain
{
    "code": 40027,
    "data": {
        "srv_ssn_id": 18794021,
        "sub_id": 132922861,
        "tran_ssn_id": 117
    },
    "id": "6742284026339393536",
    "message": "转移会话已经完成",
    "type": "resp.transfer.accept"
}
```

### 21. 会话转移拒绝-接入方
有会话转移接入，选择拒绝

#### a. 发送
消息类型：`transfer.reject`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| id | string | 无 |
| type | string | 无 |
| - data | object | 无 |
| tran_ssn_id | number | 会话转移id |
| srv_ssn_id | number | 服务会话id |
| sub_id | number | 客户id |
| reason | string | 拒绝原因 |


##### JSON格式
```plain
{
    "id": "",
    "type": "transfer.reject",
    "data": {
        "tran_ssn_id": 123,
        "srv_ssn_id": 123,
        "sub_id": 13,
        "reason": "拒绝理由"
    }
}
```

#### b. 接收
消息类型：`resp.transfer.reject`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| code | number | socket消息编码 |
| - data | object | 无 |
| srv_ssn_id | number | 服务会话id |
| sub_id | number | 客户id |
| tran_ssn_id | number | 会话转移id |
| id | string | 无 |
| message | string | socket消息内容 |
| type | string | 无 |


##### JSON格式
```plain
{
    "code": 0,
    "data": {
        "srv_ssn_id": 18794023,
        "sub_id": 132922861,
        "tran_ssn_id": 119
    },
    "id": "6742285127797178368",
    "message": "ok",
    "type": "resp.transfer.reject"
}
```

### 22. 会话转移被接受通知-发起方
会话转出已经被目标客服接受

#### a. 发送
消息类型：`transfer.accepted`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| - data | object | 无 |
| dst_emp_node_code | string | 目标客服所属网点代码 |
| srv_ssn_id | number | 服务会话id |
| dst_emp_name | string | 目标客服姓名 |
| openid | string | 渠道客户id |
| sub_id | number | 客户id |
| platform_name | string | 渠道中文名 |
| src_emp_code | string | 发起源客服工号 |
| tran_ssn_id | number | 会话转移id |
| dst_emp_code | string | 目标客服工号 |
| ent_code | string | 企业代码 |
| platform | string | 渠道 |
| app_code | string | 渠道代码 |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "data": {
        "dst_emp_node_code": "999999",
        "srv_ssn_id": 18794019,
        "dst_emp_name": "唐亮",
        "openid": "jar-9akcyfbrhgb18rltjfm6aqahos1c4jrn_c-n",
        "sub_id": 132922861,
        "platform_name": "官网在线",
        "src_emp_code": "00397589",
        "tran_ssn_id": 115,
        "dst_emp_code": "00069119",
        "ent_code": "yto",
        "platform": "site",
        "app_code": "site_yto"
    },
    "id": "6742281937735385088",
    "type": "transfer.accepted"
}
```

### 23. 会话转移被拒绝通知-发起方
会话转出已经被目标客服拒接

#### a. 发送
消息类型：`transfer.rejected`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| - data | object | 无 |
| dst_emp_node_code | string | 目标客服所属网点代码 |
| srv_ssn_id | number | 服务会话id |
| openid | string | 渠道客服id |
| sub_id | number | 客户id |
| platform_name | string | 渠道中文名 |
| src_emp_code | string | 发起源客服工号 |
| rej_reason | string | 拒绝原因 |
| tran_ssn_id | number | 会话转移id |
| dst_emp_code | string | 目标客服工号 |
| ent_code | string | 企业代码 |
| platform | string | 渠道 |
| app_code | string | 渠道代码 |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "data": {
        "dst_emp_node_code": "999999",
        "srv_ssn_id": 18794023,
        "openid": "jar-9akcyfbrhgb18rltjfm6aqahos1c4jrn_c-n",
        "sub_id": 132922861,
        "platform_name": "官网在线",
        "src_emp_code": "00397589",
        "rej_reason": "111",
        "tran_ssn_id": 119,
        "dst_emp_code": "00069119",
        "ent_code": "yto",
        "platform": "site",
        "app_code": "site_yto"
    },
    "id": "6742285127881064448",
    "type": "transfer.rejected"
}
```

### 26. websocket连接失败
wesocket连接服务器时ticket认证失败，需重新登录获取

#### a. 接收
消息类型：`auth.failed`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| code | number | 无 |
| - data | object | 无 |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "code": 60002,
    "data": {},
    "id": "",
    "type": "auth.failed"
}
```

```

### 27. websocket退出登录
websocket收到退出登录命令，并被服务器主动断开

#### a. 接收
消息类型：`system.logout`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| code | number | 无 |
| - data | object | 无 |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "code": 0,
    "data": {},
    "id": "1607415824894",
    "type": "system.logout"
}
```

```

### 28. 辅助话术回复
websocket收到辅助话术命令，在聊天窗口显示辅助话术窗口，以供客服选择

#### a. 接收
消息类型：`assist.reply`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| data | object | 无 |
| - assistId | number | 辅助id |
| - customerId | number | 客户id |
| - customerSessionId | number | 客户会话id |
| - msgList | Array | 关联消息id列表 |
| - customerAsk | String | 客户问题 |
| - assistReplyList | Array | 辅助话术列表 |
| - - assistReply | string | 辅助话术 |
| - - manualReply | string | 人工回复，仅拉历史记录时显示 |
| - - adopt | bool | 是否采纳，仅拉历史记录时显示 |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "data": {
        "assistId": 1665978639720124417,
        "customerId": 132946037,
        "customerSessionId": 20189969,
        "msgList": [
            268111501
        ],
        "customerAsk": "催件",
        "assistReplyList": [
            {
                "assistReply": "亲，麻烦提供一下您的姓名、电话，这边帮您登记要求网点尽快给您处理哦～"
            },
            {
                "assistReply": "亲，麻烦提供一下您的姓名～"
            }
        ]
    },
    "id": "1686035234838",
    "type": "assist.reply"
}
```

#### b. 响应
消息类型：`assist.reply.resp`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| data | object | 无 |
| - assistId | number | 辅助id |
| - receiveTime | number | 接收时间 |
| type | string | 无 |


##### JSON格式
```plain
{
    "type": "assist.reply.resp"
    "data": {
        "assistId": 1665978639720124417,
        "receiveTime": 1607490339184
    }
}
```

### 29. 三分钟超时提醒
客服说话后，客户在三分钟之内没有说话，发送提醒消息给双方。

#### a. 接收
消息类型：`session.timeout`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| - data | object | 无 |
| app_code | string | 渠道代码 |
| category | string | 消息业务类型：<br/>autreq自助请求<br/>autres自助回复<br/>watreq排队请求<br/>watres排队回复<br/>manreq人工请求<br/>manres人工回复<br/>sys_ssncls系统关闭<br/>surreq客户评价<br/>ssn_timeout系统回复 |
| create_at | number | 创建时间，时间戳 |
| ent_code | string | 企业代码 |
| - - msg_body | object | 消息体 |
| content | string | 文本内容 |
| msg_id | number | 消息id |
| msg_type | string | 消息类型 |
| openid | string | 渠道客户id |
| platform | string | 渠道名称 |
| send_at | number | 发送时间，时间戳 |
| sub_id | number | 客户id |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "data": {
        "app_code": "site_yto",
        "category": "ssn_timeout",
        "create_at": 1585014408,
        "ent_code": "yto",
        "msg_body": {
            "content": "亲，您已经很长时间没有说话咯！会话将在两分钟之内关闭哦！（温馨提示：如会话关闭后问题还未解决，您可以通过圆通官网www.yto.net.cn在线客服跟我们取得联系，也可以拨打%s联系我们，感谢您的理解和支持！）"
        },
        "msg_id": 267548455,
        "msg_type": "text",
        "openid": "yipkhezsgf7n9o4lwdp3lh3jc0nf9vvhg3zcbics",
        "platform": "site",
        "send_at": 1585014408,
        "sub_id": 132922995
    },
    "id": "6650662839587438592",
    "type": "session.timeout"
}
```

### 30. 八秒、二十秒兜底话术
客户说话后，客服在8秒、20秒内未回复，系统自动发送兜底话术(分别触发1次)

#### a. 接收
消息类型：`substitute.reply`

##### 参数列表
| _**参数**_ | _**类型**_ | _**描述**_ |
| :--- | :--- | :--- |
| - data | object | 无 |
| app_code | string | 渠道代码 |
| category | string | 消息业务类型：<br/>autreq自助请求<br/>autres自助回复<br/>watreq排队请求<br/>watres排队回复<br/>manreq人工请求<br/>manres人工回复<br/>sys_ssncls系统关闭<br/>surreq客户评价<br/>ssn_timeout系统回复 |
| create_at | number | 创建时间，时间戳 |
| ent_code | string | 企业代码 |
| - - msg_body | object | 消息体 |
| content | string | 文本内容 |
| msg_id | number | 消息id |
| msg_type | string | 消息类型 |
| openid | string | 渠道客户id |
| platform | string | 渠道名称 |
| send_at | number | 发送时间，时间戳 |
| sub_id | number | 客户id |
| id | string | 无 |
| type | string | 无 |


##### JSON格式
```plain
{
    "data": {
        "app_code": "site_yto",
        "category": "manres",
        "create_at": 1585014408,
        "ent_code": "yto",
        "msg_body": {
            "content": "您好，请稍等，小圆马上帮您查询核实，可能需要1-2分钟"
        },
        "msg_id": 267548455,
        "msg_type": "text",
        "openid": "yipkhezsgf7n9o4lwdp3lh3jc0nf9vvhg3zcbics",
        "platform": "site",
        "send_at": 1585014408,
        "sub_id": 132922995
    },
    "id": "6650662839587438592",
    "type": "substitute.reply"
}
```


