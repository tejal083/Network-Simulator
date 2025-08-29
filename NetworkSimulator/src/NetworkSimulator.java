import java.util.*;
import physicallayer.*;
import datalink.*;
public class NetworkSimulator {
    static Scanner sc = new Scanner(System.in);

    static class Device {
        String ipv4;
        String mac;
        String subnetMask;
        int id;

        public Device(int id, String ipv4, String subnetMask, String mac) {
            this.id = id;
            this.ipv4 = ipv4;
            this.subnetMask = subnetMask;
            this.mac = mac;
        }

        @Override
        public String toString() {
            return "ID:" + id + " IP: " + ipv4 + "/" + subnetMask + ", MAC: " + mac;
        }
    }

    static Map<String, String> arpCache = new HashMap<>();
    static int INF = 999999;
    static int[][] graph;
    static Map<Integer, Map<Integer, Integer>> routingTables = new HashMap<>();

    public static void main(String[] args) {
        System.out.print("Enter the number of devices: ");
        int n = sc.nextInt();
        sc.nextLine();

        System.out.print("Enter subnet prefix length (e.g. 24 for 255.255.255.0): ");
        int prefixLength = sc.nextInt();
        sc.nextLine();

        String subnetMask = prefixLengthToMask(prefixLength);

        List<Device> devices = new ArrayList<>();

        System.out.println("\nGenerating devices...");
        for (int i = 0; i < n; i++) {
            String ip = generateRandomIP(prefixLength);
            String mac = generateRandomMac();
            Device d = new Device(i, ip, subnetMask, mac);
            devices.add(d);
            arpCache.put(ip, mac);
            System.out.println(d);
        }

        graph = new int[n][n];
        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) graph[i][j] = 0;
                else {
                    graph[i][j] = (rand.nextInt(10) < 7) ? rand.nextInt(9) + 1 : INF;
                }
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                graph[j][i] = graph[i][j];
            }
        }

        System.out.println("\nChoose routing type:\n1. Static Routing\n2. Dynamic Routing");
        int routingChoice = sc.nextInt();
        sc.nextLine();

        if (routingChoice == 1) {
            System.out.println("\n--- Static Routing Selected ---");
            performStaticRouting(devices);
            displayArpCache();
        } else if (routingChoice == 2) {
            System.out.println("\n--- Dynamic Routing Selected ---");
            System.out.println("Choose protocol:\n1. RIP (Bellman-Ford)\n2. OSPF (Dijkstra)");
            int protocolChoice = sc.nextInt();
            sc.nextLine();

            if (protocolChoice == 1) {
                System.out.println("\nPerforming Dynamic Routing using RIP (Bellman-Ford)...");
                performRIP(devices);
            } else if (protocolChoice == 2) {
                System.out.println("\nPerforming Dynamic Routing using OSPF (Dijkstra)...");
                performOSPF(devices);
            } else {
                System.out.println("Invalid choice!");
                return;
            }
        } else {
            System.out.println("Invalid choice!");
            return;
        }

        System.out.print("\nEnter source device IP: ");
        String srcIP = sc.nextLine();
        System.out.print("Enter destination device IP: ");
        String destIP = sc.nextLine();

        Device srcDevice = findDeviceByIP(devices, srcIP);
        Device destDevice = findDeviceByIP(devices, destIP);

        if (srcDevice == null || destDevice == null) {
            System.out.println("Invalid source or destination IP!");
            return;
        }

        System.out.println("\nDelivering data from " + srcIP + " to " + destIP + "...");
        List<Integer> path = findPath(srcDevice.id, destDevice.id);
        if (path == null || path.size() == 0) {
            System.out.println("No path found from source to destination!");
        } else {
            System.out.println("Delivery path (with ARP lookups):");
            for (int i = 0; i < path.size(); i++) {
                Device device = devices.get(path.get(i));
                String mac = resolveMac(device.ipv4);
                if (mac == null) {
                    System.out.println("ARP request for " + device.ipv4 + " failed. Cannot deliver data.");
                    return;
                }
                System.out.println("Hop " + (i + 1) + ": IP: " + device.ipv4 + " -> MAC: " + mac);
            }
            System.out.println("Data successfully delivered.");
        }
    }

    static String prefixLengthToMask(int prefix) {
        int mask = 0xffffffff << (32 - prefix);
        int[] parts = new int[4];
        for (int i = 0; i < 4; i++) {
            parts[i] = (mask >> (24 - i * 8)) & 0xff;
        }
        return parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
    }

    static String generateRandomIP(int prefixLength) {
        Random rand = new Random();
        int first = 192, second = 168;
        int third = 0, fourth = 0;

        if (prefixLength <= 16) {
            third = rand.nextInt(256);
            fourth = rand.nextInt(256);
        } else if (prefixLength <= 23) {
            third = rand.nextInt(256);
            int hostBits = 32 - prefixLength;
            int maxHost = (int) Math.pow(2, hostBits) - 2;
            fourth = rand.nextInt(maxHost) + 1;
        } else {
            third = rand.nextInt(256);
            fourth = rand.nextInt(254) + 1;
        }
        return first + "." + second + "." + third + "." + fourth;
    }

    static String generateRandomMac() {
        Random rand = new Random();
        byte[] macAddr = new byte[6];
        rand.nextBytes(macAddr);
        macAddr[0] = (byte) (macAddr[0] & (byte) 254);
        macAddr[0] = (byte) (macAddr[0] | (byte) 2);

        StringBuilder sb = new StringBuilder(18);
        for (byte b : macAddr) {
            if (sb.length() > 0) sb.append(":");
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    static void performStaticRouting(List<Device> devices) {
        System.out.println("Performing static routing (manual routes)...");
        routingTables.clear();

        for (int i = 0; i < devices.size(); i++) {
            Map<Integer, Integer> routingTable = new HashMap<>();
            for (int j = 0; j < devices.size(); j++) {
                if (i != j) routingTable.put(j, j);
            }
            routingTables.put(i, routingTable);
        }

        for (int i = 0; i < devices.size(); i++) {
            Device d1 = devices.get(i);
            System.out.println("\nRouting table for device " + d1.ipv4);
            Map<Integer, Integer> rt = routingTables.get(i);
            for (Map.Entry<Integer, Integer> entry : rt.entrySet()) {
                Device dest = devices.get(entry.getKey());
                Device nextHop = devices.get(entry.getValue());
                System.out.println("Route to " + dest.ipv4 + " via " + nextHop.ipv4);
            }
        }
    }

    static void displayArpCache() {
        System.out.println("\nARP Cache:");
        for (Map.Entry<String, String> entry : arpCache.entrySet()) {
            System.out.println("IP: " + entry.getKey() + " -> MAC: " + entry.getValue());
        }
    }

    static void performRIP(List<Device> devices) {
        routingTables.clear();
        int n = devices.size();
        for (Device source : devices) {
            System.out.println("Routing table for device " + source.ipv4 + " (RIP):");
            int[] dist = new int[n];
            int[] nextHop = new int[n];
            Arrays.fill(dist, INF);
            Arrays.fill(nextHop, -1);
            dist[source.id] = 0;
            nextHop[source.id] = source.id;

            for (int i = 1; i < n; i++) {
                for (int u = 0; u < n; u++) {
                    for (int v = 0; v < n; v++) {
                        if (graph[u][v] != INF && dist[u] != INF && dist[u] + graph[u][v] < dist[v]) {
                            dist[v] = dist[u] + graph[u][v];
                            nextHop[v] = u;
                        }
                    }
                }
            }

            Map<Integer, Integer> routingTable = new HashMap<>();
            for (int dest = 0; dest < n; dest++) {
                if (dest == source.id) continue;
                int hop = dest;
                while (nextHop[hop] != source.id && nextHop[hop] != hop && nextHop[hop] != -1) {
                    hop = nextHop[hop];
                }
                if (nextHop[hop] == -1) hop = -1;
                routingTable.put(dest, hop);
                String nextHopIp = (hop != -1) ? devices.get(hop).ipv4 : "N/A";
                System.out.printf("Dest: %s, Cost: %d, NextHop: %s\n", devices.get(dest).ipv4, dist[dest], nextHopIp);
            }
            routingTables.put(source.id, routingTable);
            System.out.println();
        }
    }

    static void performOSPF(List<Device> devices) {
        routingTables.clear();
        int n = devices.size();
        for (Device source : devices) {
            System.out.println("Routing table for device " + source.ipv4 + " (OSPF):");
            int[] dist = new int[n];
            int[] prev = new int[n];
            boolean[] visited = new boolean[n];
            Arrays.fill(dist, INF);
            Arrays.fill(prev, -1);
            dist[source.id] = 0;

            for (int i = 0; i < n; i++) {
                int u = -1, minDist = INF;
                for (int j = 0; j < n; j++) {
                    if (!visited[j] && dist[j] < minDist) {
                        minDist = dist[j];
                        u = j;
                    }
                }
                if (u == -1) break;
                visited[u] = true;

                for (int v = 0; v < n; v++) {
                    if (!visited[v] && graph[u][v] != INF) {
                        int alt = dist[u] + graph[u][v];
                        if (alt < dist[v]) {
                            dist[v] = alt;
                            prev[v] = u;
                        }
                    }
                }
            }

            Map<Integer, Integer> routingTable = new HashMap<>();
            for (int dest = 0; dest < n; dest++) {
                if (dest == source.id) continue;
                int nextHop = dest;
                while (prev[nextHop] != source.id && prev[nextHop] != -1) {
                    nextHop = prev[nextHop];
                }
                if (prev[nextHop] == -1) nextHop = -1;
                routingTable.put(dest, nextHop);
                String nextHopIp = (nextHop != -1) ? devices.get(nextHop).ipv4 : "N/A";
                System.out.printf("Dest: %s, Cost: %d, NextHop: %s\n", devices.get(dest).ipv4, dist[dest], nextHopIp);
            }
            routingTables.put(source.id, routingTable);
            System.out.println();
        }
    }

    static Device findDeviceByIP(List<Device> devices, String ip) {
        for (Device d : devices) {
            if (d.ipv4.equals(ip)) return d;
        }
        return null;
    }

    static List<Integer> findPath(int sourceId, int destId) {
        List<Integer> path = new ArrayList<>();
        path.add(sourceId);
        int current = sourceId;
        while (current != destId) {
            Map<Integer, Integer> rt = routingTables.get(current);
            if (rt == null) return null;
            Integer nextHop = rt.get(destId);
            if (nextHop == null || nextHop == current) return null;
            path.add(nextHop);
            current = nextHop;
            if (path.size() > routingTables.size()) return null;
        }
        return path;
    }

    static String resolveMac(String ip) {
        String mac = arpCache.get(ip);
        if (mac == null) {
            System.out.println("Sending ARP request for IP: " + ip);
        }
        return mac;
    }
}
