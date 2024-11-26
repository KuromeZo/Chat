Chat Server
This is a simple multi-user chat server written in Java. It allows users to connect, send messages, and use various chat commands such as showing blocked phrases, listing all users, and sending private messages.

Features:
) User Registration: Users are prompted to enter a username, which must be unique.
) Messaging: Users can send messages to everyone or to specific users (private messages).
) Commands:
\blocked: Display a list of banned phrases.
\allUsers: Show the list of all connected users.
\exit: Disconnect from the chat server.
Private messaging: Use the format \username: message to send messages to specific users.
Broadcast to all except some users: Use the format -\username1, username2: message.

Setup
Clone this repository.
Create a server_config.properties file with the following fields:
) port: The port on which the server will run.
) server_name: The name of your server.
) banned_phrases: Comma-separated list of banned phrases.
Run ChatServer.java to start the server.

Usage
Run the server using java ChatServer.
Users can connect via a chat client.
After connecting, users will be asked to enter their username.
Once connected, users can start sending messages and using commands.
