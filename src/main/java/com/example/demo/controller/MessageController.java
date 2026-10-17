package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.SendMessageRequest;
import com.example.demo.dto.UserVO;
import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "消息接口")
@RestController
@RequestMapping("/api/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @Operation(summary = "搜索用户（发起聊天用）")
    @GetMapping("/search-users")
    public Result<List<UserVO>> searchUsers(@RequestAttribute("userId") Long userId,
                                             @RequestParam String kw) {
        List<User> users = messageService.searchUsers(kw, userId);
        return Result.success(users.stream().map(UserVO::from).toList());
    }

    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public Result<Message> send(@RequestAttribute("userId") Long userId,
                                @Valid @RequestBody SendMessageRequest req) {
        return Result.success(messageService.sendUserMessage(userId, req.getReceiverId(), req.getContent()));
    }

    @Operation(summary = "会话列表")
    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> conversations(@RequestAttribute("userId") Long userId) {
        return Result.success(messageService.getConversations(userId));
    }

    @Operation(summary = "和某人的聊天记录")
    @GetMapping("/history")
    public Result<List<Message>> history(@RequestAttribute("userId") Long userId,
                                         @RequestParam Long userId2) {
        return Result.success(messageService.getChatHistory(userId, userId2));
    }

    @Operation(summary = "标某会话已读")
    @PostMapping("/read")
    public Result<Integer> markRead(@RequestAttribute("userId") Long userId,
                                     @RequestParam Long conversationUserId) {
        if (conversationUserId == 0L) {
            return Result.success(messageService.markAllSystemRead(userId));
        }
        return Result.success(messageService.markConversationRead(userId, conversationUserId));
    }

    @Operation(summary = "未读消息总数")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestAttribute("userId") Long userId) {
        return Result.success(messageService.getUnreadCount(userId));
    }

    @Operation(summary = "系统通知列表")
    @GetMapping("/system")
    public Result<List<Message>> system(@RequestAttribute("userId") Long userId) {
        return Result.success(messageService.getSystemMessages(userId));
    }
}