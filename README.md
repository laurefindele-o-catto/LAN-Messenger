LAN Messenger (JavaFX)
A lightweight LAN-based instant messaging application built with JavaFX and Java Sockets.
It enables real-time private and group communication over a local network, with features like user authentication, friend requests, and profile image uploads.

🚀 Features
- User Authentication: Secure signup and login with server-side validation
- Real-Time Messaging: Private and group chat with persistent history
- Friend System: Send, accept, or decline friend requests with instant notifications
- Profile Management: Store profile pictures and detailed user information
- File & Media Sharing: Upload and share files, photos, and documents seamlessly
- Video Chat: Peer-to-peer video calling over LAN for richer communication
- Scalable Server: Thread pool–based architecture for handling multiple clients efficiently
- Event Dispatching: Centralized message routing with ordered delivery to prevent race conditions
- Group Communication: Create and manage chat groups with persistent membership tracking
- Cross-Platform UI: Built with JavaFX for a responsive and modern desktop interface
  

🛠️ Tech Stack
- JavaFX – UI framework
- Java Sockets – Client-server communication
- ExecutorService – Thread pool for scalability
- ConcurrentHashMap – Managing online users and groups


🏗️ Architecture Overview
The application follows a client–server model with centralized connection management:
Server:
- Runs a TCP server on port 12346
- Uses a fixed thread pool to handle multiple clients efficiently
- Manages online users, groups, and persistent chat/file storage
- Handles message routing, broadcasts, and file/media uploads
Client (JavaFX):
- Controllers handle UI interactions (signup, chat, friend requests, etc.)
- ClientConnection (singleton) maintains one persistent socket per user
- Routes incoming messages/events to registered listeners
- Ensures ordered delivery of messages to prevent race conditions
- Supports private chat, group chat, file sharing, and video calls

Message Flow
User Input → ClientConnection.send()
     ↓
Server ClientHandler processes & forwards
     ↓
ClientConnection (receiver) dispatches
     ↓
UI Controller updates JavaFX interface



📦 Installation
- Clone the repository:
git clone https://github.com/your-username/lan-messenger.git
cd lan-messenger

- Ensure you have Java 11+ and JavaFX SDK installed.
- Build and run the server
- Build and run the client

  🤝 Contributing
Contributions are welcome! Please fork the repository and submit a pull request with improvements or bug fixes.

📜 License
This project is licensed under the MIT License. 

