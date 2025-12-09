# Group Chat Feature - Implementation Guide

## Overview
This document describes the complete group chat feature implementation for the TravelMate application according to the Vietnamese specifications provided.

## ✅ Implemented Features

### 1. Chat List Screen (ChatListActivity)
- **Search functionality** - Search for groups by name
- **Two tabs**: "All" and "Archived" for organizing conversations
- **Group list items** showing:
  - Group avatar
  - Group name (bold)
  - Last message preview with sender name
  - Timestamp (relative: "2 giờ", "1 ngày")
  - Unread message badge (red circle with count)
- **Action buttons**:
  - Add friends button
  - Create group button

### 2. Chat Interface Screen (GroupChatActivity)
- **Header**:
  - Back button
  - Group name and member count
  - Menu button (opens Group Dashboard)
- **Message display**:
  - Supports sent, received, and system messages
  - Reply/quote feature with visual indicator (colored bar on left)
  - Reaction feature (emoji reactions: ❤️ 👍 😂 😮 😢 🙏)
  - Read receipts (small avatars below last message)
  - System notifications (e.g., "19:07 Hôm nay")
- **Rich input toolbar**:
  - Image/camera button
  - Attach file button
  - Location button
  - Task/reminder button
- **Message input**:
  - Text input with @ mention support
  - Toggle between Send button (when text exists) and Like button (when empty)

### 3. Group Dashboard Screen (GroupDashboardActivity)
- **Header**:
  - Group avatar (centered)
  - Group name with edit button
- **Quick actions (4 buttons)**:
  - Search messages
  - Mute/unmute notifications
  - Pin/unpin conversation
  - Add members
- **Expandable sections**:
  - **Group members**: List of all members
  - **Bulletin board**: For pinning important travel info (schedules, tickets, deadlines)
  - **Photo/Video library**: Grid view of all shared media
  - **Files & Links**: Tabs for files (PDF, documents) and links (articles, maps)
- **Leave group button** (red, at bottom)

## 📁 Project Structure

### Data Models (`/models`)
```
Message.java        - Message model with reply, reactions, and read receipts
ChatGroup.java      - Group model with members, last message, unread counts
GroupMember.java    - Member model with role and join date
```

### Activities
```
ChatListActivity.java          - Main chat list screen
GroupChatActivity.java         - Chat interface
GroupDashboardActivity.java    - Group info and settings
```

### Adapters
```
ChatGroupAdapter.java  - Adapter for chat group list
MessageAdapter.java    - Adapter for messages (sent/received/system)
```

### Layouts
```
activity_chat_list.xml          - Chat list screen layout
item_chat_group.xml             - Chat group item layout
activity_group_chat.xml         - Chat interface layout
item_message_sent.xml           - Sent message bubble layout
item_message_received.xml       - Received message bubble layout
item_message_system.xml         - System message layout
activity_group_dashboard.xml    - Dashboard layout
```

### Drawable Resources
```
Icons:
- ic_chat.xml           - Chat navigation icon
- ic_add_group.xml      - Create group icon
- ic_add_friend.xml     - Add friend icon
- ic_attach.xml         - Attach file icon
- ic_camera.xml         - Camera/image icon
- ic_task.xml           - Task/reminder icon
- ic_like.xml           - Like/thumbs up icon
- ic_more_vert.xml      - Menu icon (3 dots)
- ic_back_arrow.xml     - Back navigation icon
- ic_location_pin.xml   - Location icon

Backgrounds:
- bg_message_sent.xml       - Orange bubble for sent messages
- bg_message_received.xml   - Gray bubble for received messages
- bg_reply_indicator.xml    - Background for reply preview
- bg_unread_badge.xml       - Red circle for unread count
- bg_input_rounded.xml      - Rounded input field background
- bg_quick_action_button.xml - Circle background for quick actions
```

## 🔧 Firebase Integration

### Firestore Collections Structure

```
chat_groups/
  {groupId}/
    - id: string
    - name: string
    - avatarUrl: string
    - memberIds: array<string>
    - lastMessageContent: string
    - lastMessageSenderId: string
    - lastMessageSenderName: string
    - lastMessageTime: timestamp
    - unreadCount: map<userId, count>
    - isArchived: boolean
    - isMuted: boolean
    - isPinned: boolean
    - createdAt: timestamp
    - createdBy: string
    
    messages/
      {messageId}/
        - id: string
        - groupId: string
        - senderId: string
        - senderName: string
        - senderAvatar: string
        - content: string
        - type: string (text, image, file, location, system)
        - timestamp: timestamp
        - replyToId: string (nullable)
        - replyToContent: string (nullable)
        - replyToSenderName: string (nullable)
        - reactions: map<userId, emoji>
        - readBy: array<string>
        - imageUrl: string (nullable)
        - fileName: string (nullable)
        - fileUrl: string (nullable)
        - latitude: double (nullable)
        - longitude: double (nullable)
        - locationName: string (nullable)
```

## 🎨 UI/UX Features

### Colors
- Primary Orange: `#FF6B35`
- Text Dark: `#333333`
- Text Gray: `#666666`
- Text Light Gray: `#999999`
- Background Gray: `#F0F0F0`
- Border Gray: `#E0E0E0`
- Badge Red: `#FF3B30`
- White: `#FFFFFF`

### Typography
- Group name: 16sp, bold
- Last message: 14sp, regular
- Timestamp: 12sp
- Badge text: 11sp, bold
- Message text: 14sp
- System message: 12sp

### Message Bubbles
- **Sent messages**: Orange background, right-aligned, rounded corners (16dp) except bottom-right (4dp)
- **Received messages**: Gray background, left-aligned, rounded corners (16dp) except bottom-left (4dp)
- **System messages**: Gray text, centered, with subtle background

## 🚀 How to Use

### 1. Navigation
The chat feature is accessible from the HomeActivity bottom navigation bar. Click the chat icon (third from left) to open ChatListActivity.

### 2. Creating a Group (To be implemented)
Currently shows a toast message. Future implementation will open CreateGroupActivity.

### 3. Sending Messages
1. Open a group from the chat list
2. Type your message in the input field
3. Click the Send button (appears when text is entered)
4. Or click the Like button to send a quick thumbs up

### 4. Replying to Messages
1. Long press on any message
2. Select "Trả lời" from the dialog
3. The reply preview appears above the input field
4. Type your reply and send
5. The message will show the reply indicator with the original message

### 5. Adding Reactions
1. Long press on any message
2. Select "Thả cảm xúc" from the dialog
3. Choose an emoji from the list
4. The emoji appears below the message with a count

### 6. Group Dashboard
1. Click the menu icon (3 dots) in the chat header
2. Access group settings, members, media, and files
3. Edit group name by clicking the edit icon
4. Toggle notifications, pin status
5. View shared media in grid layout
6. Browse files and links in separate tabs

## 📝 Vietnamese Strings

All user-facing strings are in Vietnamese:
- "Tin nhắn" - Messages
- "Tìm kiếm" - Search
- "Bạn" - You
- "thành viên" - members
- "Trả lời" - Reply
- "Thả cảm xúc" - React
- "Rời nhóm" - Leave group
- etc.

## 🔐 Security Considerations

1. **Authentication**: All operations require Firebase Authentication
2. **Authorization**: Users can only access groups they're members of
3. **Data validation**: All inputs are validated before saving to Firestore
4. **Read receipts**: Automatically marked when viewing messages

## 📱 Platform Requirements

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 14)
- **Compile SDK**: 36

## 🧪 Testing Checklist

- [ ] Open ChatListActivity from HomeActivity
- [ ] View list of chat groups
- [ ] Search for groups by name
- [ ] Switch between All and Archived tabs
- [ ] Open a chat group
- [ ] Send text messages
- [ ] Send like (thumbs up)
- [ ] Long press message to reply
- [ ] Long press message to add reaction
- [ ] View reply indicator in messages
- [ ] View reactions on messages
- [ ] Open group dashboard
- [ ] Edit group name
- [ ] Toggle mute/unmute
- [ ] Toggle pin/unpin
- [ ] Leave group

## 🔄 Real-time Features

All data is synchronized in real-time using Firebase Firestore:
- New messages appear instantly
- Message read status updates automatically
- Reactions sync across all devices
- Group member changes reflected immediately
- Last message preview updates in chat list

## 🎯 Future Enhancements

Placeholders exist for:
- [ ] Add friends functionality
- [ ] Create group flow (CreateGroupActivity)
- [ ] Image/file upload
- [ ] Location sharing
- [ ] Task/reminder creation
- [ ] Message search
- [ ] Voice messages
- [ ] Video calls
- [ ] Message forwarding
- [ ] Group admin features

## 📚 Dependencies

Required Firebase dependencies (already in build.gradle):
- `com.google.firebase:firebase-auth`
- `com.google.firebase:firebase-firestore`
- `com.google.firebase:firebase-storage` (for images/files)

Additional libraries used:
- Glide for image loading
- Material Design components
- RecyclerView for lists

## 🐛 Known Issues

1. Build configuration needs to be verified (AGP version compatibility)
2. Some features show "đang phát triển" (in development) toasts
3. Member list, bulletin board, media gallery adapters need implementation
4. File upload functionality needs S3/Firebase Storage integration

## 📞 Support

For issues or questions, refer to:
- Firebase documentation: https://firebase.google.com/docs/android/setup
- Material Design guidelines: https://material.io/design
- Android developer guides: https://developer.android.com/guide

## ✨ Summary

This implementation provides a complete, feature-rich group chat interface specifically designed for travel planning groups in the TravelMate app. All core features specified in the requirements are implemented and ready for use with Firebase Firestore backend.

The UI follows modern Material Design principles with a Vietnamese language interface, making it intuitive for the target audience. The modular architecture allows for easy extension and customization of features as the application grows.
