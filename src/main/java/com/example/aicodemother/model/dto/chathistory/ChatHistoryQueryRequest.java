package com.example.aicodemother.model.dto.chathistory;

import com.example.aicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    //id
    private Long id;
    //应用id
    private Long appId;
    //用户id
    private Long userId;
    //消息类型
    private String messageType;
    //消息内容
    private String messageContent;
    //最后创建时间
    private LocalDateTime lastCreateTime;


    private static final long serialVersionUID = 1L;
}

