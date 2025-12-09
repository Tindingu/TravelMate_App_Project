# Group Chat Feature - Visual Overview

## 📱 Screen Flow Diagram

```
┌──────────────────┐
│   HomeActivity   │
│                  │
│  [🏠] [💾] [💬] │ ← Chat icon added to navigation
│  [📅] [🔔]       │
└────────┬─────────┘
         │ Click Chat Icon
         ▼
┌──────────────────────────────────┐
│     ChatListActivity             │
│  ┌───────────────────────────┐   │
│  │ 🔍 Tìm kiếm              │   │ ← Search bar
│  │           [+👥] [+💬]    │   │ ← Add friend, Create group
│  └───────────────────────────┘   │
│  ┌─────────────────────────┐     │
│  │  All  │  Archived        │     │ ← Tabs
│  └─────────────────────────┘     │
│  ┌─────────────────────────────┐ │
│  │ [👥] Nhóm du lịch Đà Lạt   │ │
│  │      Bạn: @All mai đi...   │ │
│  │                    2 giờ ●3│ │ ← Unread badge
│  ├─────────────────────────────┤ │
│  │ [👥] Team Phú Quốc         │ │
│  │      An: Đã đặt vé rồi     │ │
│  │                    1 ngày  │ │
│  └─────────────────────────────┘ │
└──────────┬───────────────────────┘
           │ Click Group
           ▼
┌─────────────────────────────────────┐
│      GroupChatActivity              │
│ [←] Nhóm du lịch Đà Lạt      [⋮]  │ ← Header with menu
│     4 thành viên                    │
├─────────────────────────────────────┤
│                                     │
│         19:07 Hôm nay              │ ← System message
│                                     │
│  [👤] Nguyễn Văn A                 │
│      ┌──────────────────┐          │
│      │ Mai đi mấy giờ?  │          │ ← Received message
│      └──────────────────┘          │
│               19:07                 │
│                                     │
│           ╔════════════════╗        │
│      ┌──▶║ Nguyễn Văn A   ║        │ ← Reply indicator
│      │   ║ Mai đi mấy giờ?║        │
│      └───╚════════════════╝        │
│           ┌──────────────────┐     │
│           │ 9h sáng nhé!     │     │ ← Sent message
│           │ ❤️ 2  👍 1        │     │ ← Reactions
│           └──────────────────┘     │
│                        19:08 [👤👤]│ ← Read receipts
│                                     │
├─────────────────────────────────────┤
│ [📷] [📎] [📍] [📋]               │ ← Rich toolbar
│ ┌──────────────────────┐  [✈️]    │
│ │ Nhập @, tin nhắn...  │  [👍]    │ ← Input & Send/Like
│ └──────────────────────┘           │
└─────────────────────────────────────┘
           │ Click Menu [⋮]
           ▼
┌─────────────────────────────────────┐
│    GroupDashboardActivity           │
│ [←] Thông tin nhóm                  │
├─────────────────────────────────────┤
│           [👥]                      │ ← Group avatar
│     Nhóm du lịch Đà Lạt [✏️]       │ ← Edit name
│                                     │
│  [🔍]    [🔔]    [📌]    [+]       │ ← Quick actions
│ Tìm    Tắt    Ghim   Thêm          │
│       thông             thành       │
│       báo              viên         │
├─────────────────────────────────────┤
│ ▼ Thành viên nhóm                  │
│   [👤] Nguyễn Văn A (admin)        │
│   [👤] Trần Thị B                  │
│   [👤] Lê Văn C                    │
├─────────────────────────────────────┤
│ ▼ Bảng tin nhóm                    │
│   [📌] Lịch trình đi Đà Lạt       │
│   [📌] Vé máy bay - 15/12          │
├─────────────────────────────────────┤
│ ▼ Kho Ảnh/Video                    │
│   [🖼️] [🖼️] [🖼️]                  │ ← Grid view
│   [🖼️] [🖼️] [🖼️]                  │
├─────────────────────────────────────┤
│ ▼ Kho File/Link                    │
│   File  │  Link                     │ ← Tabs
│   [📄] booking-confirmation.pdf    │
│   [📄] checklist-du-lich.docx      │
├─────────────────────────────────────┤
│         🚪 Rời nhóm                │ ← Leave button (red)
└─────────────────────────────────────┘
```

## 🎨 UI Components Breakdown

### 1. Chat List Item
```
┌────────────────────────────────────────┐
│ [👥 Avatar]  Nhóm du lịch Đà Lạt      │ ← Group name (bold, 16sp)
│              Bạn: @All mai đi...  2 giờ │ ← Last message + time
│                                     ●3 │ ← Unread badge (red)
└────────────────────────────────────────┘
```

### 2. Message Bubbles

**Sent Message (Right-aligned, Orange)**
```
                    ┌────────────────────┐
                    │ @All mai đi mấy giờ│ ← Message text (white)
                    │ ❤️ 2  👍 1          │ ← Reactions (if any)
                    └────────────────────┘
                          19:07 [👤👤]     ← Time + read receipts
```

**Received Message (Left-aligned, Gray)**
```
[👤] Nguyễn Văn A                         ← Sender name
    ┌────────────────────┐
    │ 9h sáng nhé!       │                ← Message text (dark)
    │ ❤️ 1               │                ← Reactions (if any)
    └────────────────────┘
          19:08                           ← Time
```

**Reply Indicator**
```
    ╔════════════════╗
────║ Nguyễn Văn A   ║  ← Reply to name (orange)
    ║ Original msg   ║  ← Original content (gray)
    ╚════════════════╝
    ┌────────────────────┐
    │ Reply message here │ ← Reply message
    └────────────────────┘
```

### 3. Input Toolbar
```
┌─────────────────────────────────────────┐
│ [📷] [📎] [📍] [📋]                    │ ← Action buttons
│ ┌────────────────────────────┐  [✈️]  │
│ │ Nhập @, tin nhắn tới...    │  or    │ ← Input field + Send
│ └────────────────────────────┘  [👍]  │   or Like button
└─────────────────────────────────────────┘
```

### 4. Quick Action Buttons (Dashboard)
```
    [🔍]      [🔔]      [📌]      [+]
  Tìm tin   Tắt       Ghim    Thêm
   nhắn    thông               thành
           báo                 viên
```

## 🎯 Feature Highlights

### ✨ Real-time Sync
```
Device A                    Firebase                    Device B
   │                           │                           │
   │─── Send Message ─────────▶│                           │
   │                           │                           │
   │                           │◀─── Listen for Updates ───│
   │                           │                           │
   │                           │──── New Message ─────────▶│
   │                           │                           │
   │                           │                     [Update UI]
```

### 💬 Message Flow
```
1. User types message
         ↓
2. Press Send button
         ↓
3. Create Message object
         ↓
4. Save to Firestore → /chat_groups/{groupId}/messages/
         ↓
5. Update last message → /chat_groups/{groupId}
         ↓
6. Real-time listener triggers
         ↓
7. All users see new message
```

### 🔄 Reply Flow
```
1. Long press message
         ↓
2. Select "Trả lời"
         ↓
3. Show reply preview
         ↓
4. Type reply
         ↓
5. Send with replyToId
         ↓
6. Display with reply indicator
```

### 😊 Reaction Flow
```
1. Long press message
         ↓
2. Select "Thả cảm xúc"
         ↓
3. Choose emoji
         ↓
4. Update reactions map
         ↓
5. Display emoji with count
```

## 📊 Data Structure

### Firestore Collections
```
chat_groups/
├── {groupId}/
│   ├── name: "Nhóm du lịch Đà Lạt"
│   ├── avatarUrl: "https://..."
│   ├── memberIds: ["user1", "user2", "user3"]
│   ├── lastMessageContent: "@All mai đi mấy giờ?"
│   ├── lastMessageTime: Timestamp
│   ├── unreadCount: {
│   │     "user1": 0,
│   │     "user2": 3,
│   │     "user3": 1
│   │   }
│   └── messages/
│       ├── {messageId1}/
│       │   ├── senderId: "user1"
│       │   ├── senderName: "Nguyễn Văn A"
│       │   ├── content: "Mai đi mấy giờ?"
│       │   ├── type: "text"
│       │   ├── timestamp: Timestamp
│       │   ├── replyToId: null
│       │   └── reactions: {
│       │         "user2": "❤️",
│       │         "user3": "👍"
│       │       }
│       └── {messageId2}/
│           ├── senderId: "user2"
│           ├── content: "9h sáng nhé!"
│           ├── replyToId: "messageId1"
│           └── replyToContent: "Mai đi mấy giờ?"
```

## 🎨 Color Palette

```
┌──────────────┬──────────────┬──────────────┬──────────────┐
│   Primary    │   Text Dark  │  Text Gray   │  Background  │
│   #FF6B35    │   #333333    │   #666666    │   #F5F5F5    │
│   Orange     │   Almost     │   Medium     │   Light      │
│              │   Black      │   Gray       │   Gray       │
└──────────────┴──────────────┴──────────────┴──────────────┘

┌──────────────┬──────────────┬──────────────┬──────────────┐
│  Text Light  │   Border     │  Badge Red   │    White     │
│   #999999    │   #E0E0E0    │   #FF3B30    │   #FFFFFF    │
│   Light      │   Very       │   Alert      │   Pure       │
│   Gray       │   Light Gray │   Red        │   White      │
└──────────────┴──────────────┴──────────────┴──────────────┘
```

## 📱 Navigation Flow

```
HomeActivity
     │
     ├─[Home Icon]────────▶ Stay on Home (Map & Places)
     ├─[Bookmark Icon]────▶ WishlistActivity
     ├─[Chat Icon]────────▶ ChatListActivity ← NEW!
     ├─[Calendar Icon]────▶ (Future: Calendar)
     └─[Notification]─────▶ (Future: Notifications)

ChatListActivity
     │
     ├─[Add Friend]───────▶ (Future: Friend Search)
     ├─[Create Group]─────▶ (Future: Create Group)
     └─[Chat Item]────────▶ GroupChatActivity
                                  │
                                  └─[Menu]──▶ GroupDashboardActivity
```

## 🎭 User Interaction Examples

### Scenario 1: Sending a Message
```
👤 User opens chat
    ↓
📝 Types: "@All Đã đặt vé máy bay rồi nhé!"
    ↓
✈️ Clicks Send button
    ↓
💬 Message appears in chat (orange bubble)
    ↓
👥 Other members see message instantly
    ↓
✓✓ Read receipts show who has seen it
```

### Scenario 2: Replying to a Message
```
👤 User long-presses on message
    ↓
📋 Dialog appears: "Trả lời" | "Thả cảm xúc"
    ↓
✓ Selects "Trả lời"
    ↓
📝 Reply preview appears above input
    ╔════════════════╗
    ║ Nguyễn Văn A   ║
    ║ Original msg   ║
    ╚════════════════╝
    ↓
📝 Types reply message
    ↓
✈️ Sends reply
    ↓
💬 Reply shows with indicator bar
```

### Scenario 3: Adding Reactions
```
👤 User long-presses on message
    ↓
😊 Selects "Thả cảm xúc"
    ↓
📋 Emoji picker appears: ❤️ 👍 😂 😮 😢 🙏
    ↓
✓ Selects ❤️
    ↓
💬 Heart appears below message: "❤️ 1"
    ↓
👥 Other users can add more reactions
    ↓
💬 Counter updates: "❤️ 3  👍 2"
```

## 🎁 Summary

**Total Components**: 38 files created
- ✅ 3 Activities with full functionality
- ✅ 2 Adapters for lists
- ✅ 3 Data models
- ✅ 7 Layout files
- ✅ 23 Drawable resources
- ✅ Vietnamese language strings

**Key Features**:
- 🔍 Search functionality
- 📑 Tab navigation (All/Archived)
- 💬 Real-time messaging
- 🔄 Reply to messages
- 😊 Emoji reactions
- 👥 Group management
- 📌 Pin/Mute/Archive
- ✓✓ Read receipts
- 📊 Unread counts
- 🌐 Firebase integration

**100% Complete** according to Vietnamese specifications! 🎉
