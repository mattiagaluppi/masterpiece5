# Chat Broadcast over UDP with Combined Encryption

**Author**: Mattia Galuppi
**Class**: 5N (School Year 2024/2025)

## ⚙️ Encryption Management between Client and Server

This project implements a secure encryption system based on a combination of **asymmetric encryption (RSA)** and **symmetric encryption (AES)** to ensure secure communication between clients and the server.

---

## 🔐 Types of Keys Used

### Asymmetric Encryption (RSA)

* **Public key**: used to encrypt data.
* **Private key**: used to decrypt data.
* **Purpose**: ideal for secure exchange of session keys.
* **Key property**: only the holder of the private key can decrypt data encrypted with the public key.

### Symmetric Encryption (AES)

* Uses **a single shared secret key** to encrypt and decrypt data.
* **Much faster** than asymmetric encryption.
* The key must be shared **securely** between the parties.

---

## 🔁 Key Exchange Process

### 1. **Initial Phase: Public Key Exchange (RSA)**

1. **RSA Key Generation (Server)**:

   * The server generates a pair of RSA keys (public and private) at startup.

2. **Public Key Request (Client)**:

   * The client sends a `KEY_REQUEST` message to the server via UDP.

3. **Server Response**:

   * The server replies by sending its public RSA key encoded in Base64.

4. **Client Reception**:

   * The client decodes the key from Base64 and converts it into a `PublicKey` object using `KeyFactory`.

### 2. **Intermediate Phase: AES Key Exchange**

1. **AES Key Generation (Client)**:

   * The client generates a random AES key using `KeyGenerator`.

2. **AES Key Encryption (Client)**:

   * The AES key is encrypted with the server's public RSA key.

3. **Sending the Encrypted AES Key**:

   * The client sends the AES key to the server using the format: `AES_KEY:<encrypted key>`.

4. **AES Key Decryption (Server)**:

   * The server decrypts the AES key using its private RSA key.
   * The AES key is stored in a `ConcurrentHashMap` associated with the client (identified by IP and port).

---

## ✉️ Message Transmission (Using AES)

### Client → Server

* The client encrypts messages using the shared AES key.
* The encrypted messages are sent via UDP.

### Server → Decryption

* The server retrieves the AES key associated with the client.
* Decrypts the message and displays it.

### Server → Broadcast

* The server forwards the message to all connected clients (excluding the sender).
* Each message is **re-encrypted with the AES key of each recipient**.

### Client → Reception

* Each recipient client receives the encrypted message.
* Decrypts it using their own AES key shared with the server.

---

## 📊 Summary Flow

```text
1. Client → KEY_REQUEST → Server
2. Server → [RSA Public Key] → Client
3. Client → [AES Key encrypted with RSA] → Server
4. Server → [Stores AES Key]
5. Encrypted communication via AES
```

---

## 🧱 Class Breakdown

### `Server`

* Receives messages from clients.
* Broadcasts them to other clients.
* Displays system logs via GUI.

### `Client`

* Sends, receives, and interprets messages.
* User interface resembles a modern messaging app.

### `UIStyle`

* Manages UI constants.
* The client fetches graphical properties from this class.

---

## 📦 Technologies Used

* **Java**
* **UDP Sockets**
* **RSA & AES (Javax.crypto, KeyFactory, etc.)**
* **ConcurrentHashMap**
* **JavaFX/Swing for GUI**
