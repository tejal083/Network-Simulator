# Network-Simulator
A modular simulation of OSI model layers implemented in Java. The project demonstrates networking concepts from the Physical Layer up to the Application Layer, including device connections, error handling, routing protocols, transport mechanisms, and application-level services like Telnet, FTP, and SSH.

## Features

### Physical & DATA Link Layer
- Device Connections: Direct link, Hub and Switch
- Frame creation with **MAC headers** and **parity bits**
- **CSMA/CD** for channel access control
- Error simulation with retransmission using **ARQ protocols** (Go-Back-N, Selective Repeat)
- Switch **MAC Learning** and Frame forwarding.
  
### Network Layer
- **IP Address Allocation** and Interface configuration
- **ARP resolution** 
- Static routing using routing tables
- Dynamic Routing with:
- **RIP (Bellman-Ford)**
- **OSPF (Dijkstra's Algorithm)**

### Transport Layer
- **Port Assignment**
- Reliable Delivery with **GBN ARQ**
- Segment Transmission with **Timeout retransmissions**

### Application Layer
- **Telnet**
- **FTP**
- **SSH**

## TechStack

- **Language** JAVA
- **Paradigm** Object-Oriented Programming
- **Use Cases** Educational Simulation of Computer Networks


## Getting Started

### Prerequisites
- Java 8+ installed
- Any Java IDE (Intellij, Eclipse, or VS Code)

### Run the Project
1. Clone the repository
   ''bash
   git clone https://github.com/tejal083/netowrk-simulator.git
   -cd network-simulator

3. Compile the code
   ''bash
   javac *.java
