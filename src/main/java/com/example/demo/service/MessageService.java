package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Message sendUserMessage(Long senderId, Long receiverId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException("消息内容不能超过 500 字");
        }
        if (senderId.equals(receiverId)) {
            throw new BusinessException("不能给自己发消息");
        }
        userRepository.findById(receiverId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content.trim());
        msg.setType("USER");
        return messageRepository.save(msg);
    }

    public void sendSystemMessage(Long receiverId, String content, Long relatedOrderId) {
        Message msg = new Message();
        msg.setSenderId(0L);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setType("SYSTEM");
        msg.setRelatedOrderId(relatedOrderId);
        messageRepository.save(msg);
    }

    public long getUnreadCount(Long userId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    @Transactional
    public int markConversationRead(Long userId, Long otherUserId) {
        return messageRepository.markRead(userId, otherUserId);
    }

    @Transactional
    public int markAllSystemRead(Long userId) {
        return messageRepository.markRead(userId, 0L);
    }

    public List<Map<String, Object>> getConversations(Long userId) {
        List<Message> latestList = messageRepository.findLatestPerConversation(userId);
        Map<Long, User> userCache = new HashMap<>();

        return latestList.stream()
                .map(msg -> {
                    Long otherId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
                    Map<String, Object> conv = new LinkedHashMap<>();
                    conv.put("conversationId", otherId);
                    conv.put("isSystem", otherId == 0L);
                    conv.put("latestMessage", msg.getContent());
                    conv.put("latestTime", formatTime(msg.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
                    conv.put("unreadCount", messageRepository.countUnreadFrom(userId, otherId));

                    if (otherId == 0L) {
                        conv.put("name", "系统通知");
                        conv.put("avatar", "🔔");
                    } else {
                        User u = userCache.computeIfAbsent(otherId, id -> userRepository.findById(id).orElse(null));
                        conv.put("name", u != null ? u.getUsername() : "用户已注销");
                        conv.put("avatar", u != null ? u.getUsername().substring(0, 1).toUpperCase() : "?");
                    }
                    return conv;
                })
                .sorted((a, b) -> ((String) b.get("latestTime")).compareTo((String) a.get("latestTime")))
                .collect(Collectors.toList());
    }

    public List<Message> getChatHistory(Long myId, Long otherId) {
        List<Message> sent = messageRepository.findBySenderIdAndReceiverIdOrderByCreateTimeAsc(myId, otherId);
        List<Message> received = messageRepository.findBySenderIdAndReceiverIdOrderByCreateTimeAsc(otherId, myId);
        List<Message> all = new ArrayList<>(sent);
        all.addAll(received);
        all.sort(Comparator.comparing(Message::getCreateTime));
        return all;
    }

    public List<Message> getSystemMessages(Long userId) {
        return messageRepository.findByReceiverIdOrderByCreateTimeDesc(userId).stream()
                .filter(m -> m.getType().equals("SYSTEM") || m.getSenderId() == 0L)
                .collect(Collectors.toList());
    }

    public List<User> searchUsers(String kw, Long excludeId) {
        if (kw == null || kw.trim().isEmpty()) return List.of();
        return userRepository.searchUsers(kw.trim(), excludeId);
    }

    private String formatTime(String raw) {
        return raw;
    }
}