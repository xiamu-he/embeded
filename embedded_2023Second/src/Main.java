import java.util.*;

@SuppressWarnings("unchecked")
public class Main {

    public static int N;
    public static int M;
    public static int T;
    public static int P;
    public static int R; // 路径总数R
    public static int D;
    public static ArrayList<int[]> addSide = new ArrayList<>();// 记录所加边的起止节点
    public static ArrayList<Integer>[] graphList;// 记录无重复节点的邻接表
    public static Passage[][] graph;// 所有边的邻接矩阵
    public static ArrayList<Side> sides = new ArrayList<>();// 所有边
    public static int[][] sideArray;// 边的初始化数组
    public static Business[] businessTotal;// 总业务
    public static HashSet<Integer>[] businessSideHashSet;
    public static int[] samePassBusiness;
    static boolean[] visited;
    static int[] pre;

    static ArrayList<Track>[][] minTrackWithT;// 节点间的所有最短Track

    static int[][] sideMayUseCntList = new int[5005][5005]; // 所有任务最短路路径中经过的两个直达点的次数

    static ArrayList<Map.Entry<String, ArrayList<Integer>>> entryList;// 起止点-businessId键值对

    static long totalAddSideCost;
    static long totalAmplifierCost;
    static long totalEdgeCost;
    static long totalCost;
    static final long ADD_SIDE_COST = 1000000;
    static final long AMPLIFIER_COST = 100;
    static final long EDGE_COST = 1;

    public static void main(String[] args) {
        init();

        //
        solve();

        output();
        //outCost();
    }

    public static void solve() {

        // 第一阶段
        // solveFirst();

        // 第二阶段
        solveSecond();

        // 第三阶段
        solveThird();

    }

    private static void solveFirst() {

        /*
         * 第1阶段
         * 每类业务按数量降序，并将每类业务在每个通道进行单独处理
         */
        for (int i = 0; i < entryList.size(); i++) {
            Track minTrack = null;
            for (int j = 0; j < entryList.get(i).getValue().size(); j++) {
                int p = j % P;
                int t = entryList.get(i).getValue().get(j);
                if (businessTotal[t].routeNum != 1)
                    continue;
                if (!businessTotal[t].isProcess) {
                    int startNode = businessTotal[t].startNode;
                    int endNode = businessTotal[t].endNode;
                    // 获得业务在通道p中不需要加边的最短路径
                    if (p == 0) {
                        minTrack = getPath1(startNode, endNode, p, businessTotal[t].business_id);
                    }
                    if (!minTrack.trackList.isEmpty()) {
                        // 根据路径处理对应业务
                        minTrack.p = p;
                        if (minTrack.trackList.get(0) != startNode) {
                            Collections.reverse(minTrack.trackList);
                        }
                        processBus(t, minTrack);
                    }
                }
            }
        }
    }

    private static void solveSecond() {
        ArrayList<Integer> businessList1 = sortBusiness1();
        /*
         * 第2阶段
         * 通道从小到大，在每个通道中对业务进行处理
         */
        for (int p = 0; p < P; p++) {
            for (int i = 0; i < R; i++) {
                int t = businessList1.get(i);
                int business_id = businessTotal[t].business_id;
                // 当光业务的路径数量为 2 时，这两条路径分配的通道必须相同
                if (samePassBusiness[business_id] >= 0 && samePassBusiness[business_id] != p) {
                    continue;
                }
                if (!businessTotal[t].isProcess) {
                    int startNode = businessTotal[t].startNode;
                    int endNode = businessTotal[t].endNode;
                    // 获得业务在通道p中不需要加边的最短路径
                    Track minTrack = getPath1(startNode, endNode, p, businessTotal[t].business_id);
                    // if(minTrack.trackList.isEmpty()){
                    // minTrack = getMinPathWithP(startNode,endNode,p);
                    // }
                    if (!minTrack.trackList.isEmpty()) {
                        // 根据路径处理对应业务
                        processBus(t, minTrack);
                    }
                }
            }
        }
    }

    private static void solveThird() {
        ArrayList<Integer> businessList1 = sortBusiness1();
        /*
         * 第3阶段
         * 对没有处理的业务进行遍历
         * 找不需要加边的最短路径
         * 不存在时，从最短路径中找加边最少的路径
         */
        for (int i = 0; i < R; i++) {
            int t = businessList1.get(i);
            if (!businessTotal[t].isProcess) {
                int startNode = businessTotal[t].startNode;
                int endNode = businessTotal[t].endNode;
                // 找不用加边的最短路径
                Track minTrack = getPath1(startNode, endNode, businessTotal[t].business_id);
                // 不存在时加边，从最短路径里找
                if (minTrack.trackList.isEmpty()) {
                    // 从最短路径中找加边最少的路径
                    minTrack = getPath2(startNode, endNode, businessTotal[t].business_id);
                    // 对路径进行加边
                    addSide(minTrack, t);
                }

                // 根据路径处理业务
                processBus(t, minTrack);
            }
        }
    }

    // 对业务的最短路径统计边使用数量
    public static void opBusinessMinPathNum(int task, int val) {
        int start = businessTotal[task].startNode;
        int end = businessTotal[task].endNode;
        ArrayList<Track> trackList = minTrackWithT[start][end];
        // 统计路径上点之间的使用数量
        for (var track : trackList) {
            for (int j = 0; j < track.trackList.size() - 1; j++) {
                int s1 = track.trackList.get(j);
                int e1 = track.trackList.get(j + 1);
                sideMayUseCntList[s1][e1] += val;
                sideMayUseCntList[e1][s1] += val;
            }
        }
    }

    private static void outCost() {
        for (int i = 0; i < R; i++) {
            totalAddSideCost += businessTotal[i].addSideCost;
            totalAmplifierCost += businessTotal[i].amplifierCost;
            totalEdgeCost += businessTotal[i].edgeCost;
        }
        totalCost = totalAddSideCost + totalAmplifierCost + totalEdgeCost;
        System.err.println("totalAddSideCost:" + totalAddSideCost);
        System.err.println("totalAmplifierCost:" + totalAmplifierCost);
        System.err.println("totalEdgeCost:" + totalEdgeCost);
        System.err.println("totalCost:" + totalCost);
    }

    // 对业务进行排序，按最短轨迹长度降序
    public static ArrayList<Integer> sortBusiness1() {
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < R; i++) {
            result.add(i);
        }
        result.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                int startNode1 = businessTotal[o1].startNode;
                int startNode2 = businessTotal[o2].startNode;
                int endNode1 = businessTotal[o1].endNode;
                int endNode2 = businessTotal[o2].endNode;

                int level1 = businessTotal[o1].level, level2 = businessTotal[o2].level;
                if(level1 != level2) {
                    return level2 - level1;
                    // return level1 - level2;
                }

                //     int flexibility1 = 0;
                //     int flexibility2 = 0;

                //     for (int i = 0; i < minTrackWithT[startNode1][endNode1].size(); i++) {
                //         Track track = minTrackWithT[startNode1][endNode1].get(i);
                //         flexibility1 += getSidesNum(track) / track.trackList.size();
                //     }
                //     for (int i = 0; i < minTrackWithT[startNode2][endNode2].size(); i++) {
                //         Track track = minTrackWithT[startNode2][endNode2].get(i);
                //         flexibility2 += getSidesNum(track) / track.trackList.size();
                //     }




//                int sideNum1 = 0;
//                int sideNum2 = 0;
//
//                for (int i = 0; i < o1.trackList.size() - 1; i++) {
//                    int start = o1.trackList.get(i);
//                    int end = o1.trackList.get(i + 1);
//                    sideNum1 += graph[start][end].sides.size();
//                }
//                for (int i = 0; i < o2.trackList.size() - 1; i++) {
//                    int start = o2.trackList.get(i);
//                    int end = o2.trackList.get(i + 1);
//                    sideNum2 += graph[start][end].sides.size();
//                }
//                double s1 = sideNum1 * 1.0 / o1.trackList.size();
//                double s2 = sideNum2 * 1.0 / o2.trackList.size();


                int num = minTrackWithT[startNode2][endNode2].get(0).trackList.size()
                        - minTrackWithT[startNode1][endNode1].get(0).trackList.size();
                //int num1 = num==0?hs1.size() - hs2.size() : num;

                //     int num1 = num == 0 ? flexibility1 - flexibility2 : num;
                //     // int num1 = num == 0 ? flexibility1-flexibility2 : num;
                //     int num2 = num1 == 0 ? startNode1 - startNode2 : num1;
                return num;
            }
        });
        return result;
    }

    public static int getSidesNum(Track track) {
        int result = 0;
        for (int i = 0; i < track.trackList.size() - 1; i++) {
            int start = track.trackList.get(i);
            int end = track.trackList.get(i + 1);
            result += graph[start][end].sides.size();
        }
        return result;
    }

    // 获取两点之间所有最短路径中加边最少的路径和通道
    public static Track getPath2(int startNode, int endNode, int business_id) {
        int minAddSideNum = 0;
        Track minTrack = new Track();
        ArrayList<Track> trackList = minTrackWithT[startNode][endNode];
        int x = trackList.get(0).trackList.size();
        int k = -1;
        int mi_len = Integer.MAX_VALUE;
        for (int p = 0; p < P; p++) {
            if (samePassBusiness[business_id] >= 0 && samePassBusiness[business_id] != p) {
                continue;
            }
            for (int i = 0; i < trackList.size(); i++) {

                // if(trackList.get(i).trackList.size() > x )break;
                int len = trackList.get(i).trackList.size();
                trackList.get(i).p = p;
                int addSideNum = trackList.get(i).getNeedAddSideNum(business_id);
                if (minAddSideNum + mi_len > len + addSideNum) {
                    minAddSideNum = addSideNum;
                    minTrack.p = p;
                    k = i;
                    mi_len = len;
                    // break;
                }
            }
        }
        minTrack.trackList = new ArrayList<>(trackList.get(k).trackList);

        return minTrack;
    }

    // 对应通道中能完成业务的Track
    public static Track getPath1(int startNode, int endNode, int p, int business_id) {
        Track minTrack = new Track();
        ArrayList<Track> trackList = minTrackWithT[startNode][endNode];

        // 动态排最短路径的顺序
        // trackList.sort((o1, o2) -> {
        //     int track1_useNum = 0, track2_useNum = 0;
        //     for (int i = 0; i < o1.trackList.size() - 1; i++) {
        //         int start = o1.trackList.get(i);
        //         int end = o1.trackList.get(i + 1);
        //         track1_useNum += sideMayUseCntList[start][end];
        //     }
        //     for (int i = 0; i < o2.trackList.size() - 1; i++) {
        //         int start = o2.trackList.get(i);
        //         int end = o2.trackList.get(i + 1);
        //         track1_useNum += sideMayUseCntList[start][end];
        //     }
        //     return track1_useNum - track2_useNum;
        //     // return track2_useNum - track1_useNum;
        // });

//        trackList.sort(new Comparator<Track>() {
//            @Override
//            public int compare(Track o1, Track o2) {
//                return o2.getFreeSideNum(p) - o1.getFreeSideNum(p);
//            }
//        });

        // Collections.shuffle(trackList, new Random(233));
        int x = trackList.get(0).trackList.size();
        for (int i = 0; i < trackList.size(); i++) {
            Track track = trackList.get(i);

            if (track.trackList.size() > x)
                break;
            boolean flag = true;
            for (int j = 0; j < track.trackList.size() - 1; j++) {
                int start = track.trackList.get(j);
                int end = track.trackList.get(j + 1);
                if (graph[start][end].getSideIdWithFreeP(p, business_id) == -1) {
                    // if (graph[start][end].getSideIdWithFreeP(p) == -1) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                minTrack.p = p;
                minTrack.trackList = new ArrayList<>(trackList.get(i).trackList);
                break;
            }
        }
        return minTrack;
    }

    // 获取在不加边的情况下能够完成业务的Track
    // public static Track judge_getPath1(int startNode, int endNode) {
    // int minSideNum = Integer.MAX_VALUE;
    // Track minTrack = new Track();
    // for (int p = 0; p < P; p++) {
    // Track track = getMinPathWithP(startNode, endNode, p);
    // int sideNum = track == null ? Integer.MAX_VALUE : track.trackList.size() - 1;
    // if (minSideNum > sideNum) {
    // minSideNum = sideNum;
    // minTrack.trackList = new ArrayList<>(track.trackList);
    // minTrack.p = p;
    // break;
    // }
    // }
    // return minTrack;
    // }

    // 获取在不加边的情况下能够完成业务的Track
    public static Track getPath1(int startNode, int endNode, int business_id) {
        int minSideNum = Integer.MAX_VALUE;
        Track minTrack = new Track();
        for (int p = 0; p < P; p++) {
            if (samePassBusiness[business_id] >= 0 && samePassBusiness[business_id] != p) {
                continue;
            }
            Track track = getMinPathWithP(startNode, endNode, p, business_id);
            int sideNum = track == null ? Integer.MAX_VALUE : track.trackList.size() - 1;
            if (minSideNum > sideNum) {
                minSideNum = sideNum;
                minTrack.trackList = new ArrayList<>(track.trackList);
                minTrack.p = p;
            }
        }
        return minTrack;
    }

    // 获取对应通道在不加边的情况下能够完成业务的Track
    public static Track getMinPathWithP(int startNode, int endNode, int p, int business_id) {
        pre = new int[N];
        Track track = new Track();
        visited = new boolean[N];
        Queue<Integer> q = new LinkedList<>();
        q.add(startNode);
        visited[startNode] = true;
        while (!q.isEmpty()) {
            Integer cur = q.poll();
            if (cur == endNode) {
                LinkedList<Integer> path = new LinkedList<>();
                printTrack(startNode, endNode, path);
                track.trackList = new ArrayList<>(path);
                track.p = p;
                return track;
            }
            for (int j = 0; j < graphList[cur].size(); j++) {
                int next = graphList[cur].get(j);
                if (!visited[next] && graph[cur][next].getSideIdWithFreeP(p, business_id) != -1) {
                    q.add(next);
                    visited[next] = true;
                    pre[next] = cur;
                }
            }
        }
        return null;
    }

    public static ArrayList<Integer>[] preList;

    // 获取两点间的所有的最短路径
    public static ArrayList<Track> getMinPath(int startNode, int endNode) {
        visited = new boolean[N];
        preList = new ArrayList[N];
        for (int i = 0; i < N; i++) {
            preList[i] = new ArrayList<>();
        }
        Queue<Integer> q = new LinkedList<>();
        q.add(startNode);

        while (!q.isEmpty()) {
            Integer cur = q.poll();
            visited[cur] = true;
            if (cur == endNode) {
                ArrayList<Track> res = new ArrayList<>();
                LinkedList<Integer> path = new LinkedList<>();
                getPath(startNode, endNode, path, res);
                return res;
            }
            for (int j = 0; j < graphList[cur].size(); j++) {
                int next = graphList[cur].get(j);
                if (!visited[next] && !preList[next].contains(cur)) {
                    q.add(next);
                    preList[next].add(cur);
                }
            }
        }
        return null;
    }

    // 根据preList获取两点之间的所有最短路径
    public static void getPath(int startNode, int endNode, LinkedList<Integer> track, ArrayList<Track> res) {
        track.addFirst(endNode);
        if (!res.isEmpty() && track.size() > res.get(0).trackList.size())
            return;
        if (startNode == endNode) {
            res.add(new Track(track));
            return;
        }
        for (int i = 0; i < preList[endNode].size(); i++) {
            int next = preList[endNode].get(i);
            getPath(startNode, next, track, res);
            track.removeFirst();
        }
    }

    // 对轨迹进行加边处理
    public static void addSide(Track track, int t) {
        ArrayList<Integer> sideList = track.getSideList(businessTotal[t].business_id);
        for (int j = 0; j < sideList.size(); j++) {
            int sideId = sideList.get(j);
            if (sideId == -1) {
                int start = track.trackList.get(j);
                int end = track.trackList.get(j + 1);
                M++;
                sides.add(new Side(M - 1, start, end, graph[start][end].getMinDis()));
                addSide.add(new int[] { start, end });
                graph[start][end].sides.add(M - 1);
                graph[end][start].sides.add(M - 1);
                businessTotal[t].addSideCost += ADD_SIDE_COST;
            }
        }
    }

    // 处理业务
    public static void processBus(int t, Track track) {
        // 根据加边后找到的路径处理业务
        int business_id = businessTotal[t].business_id;
        ArrayList<Integer> sideList = track.getSideList(business_id);
        opBusinessMinPathNum(t, -1);

        if (samePassBusiness[business_id] == -2) {
            samePassBusiness[business_id] = track.p;
        }

        for (int s = 0; s < sideList.size(); s++) {
            int sideId = sideList.get(s);
            // 同一个业务标记边
            businessSideHashSet[business_id].add(sideId);
            if (businessTotal[t].remainD < sides.get(sideId).dis) {
                businessTotal[t].amplifierList.add(track.trackList.get(s));
                businessTotal[t].remainD = D;
                businessTotal[t].amplifierCost += AMPLIFIER_COST;
            }
            businessTotal[t].sideList.add(sideId);
            businessTotal[t].remainD -= sides.get(sideId).dis;

            businessTotal[t].pId = track.p;
            sides.get(sideId).isUsed[track.p] = true;
            businessTotal[t].edgeCost += EDGE_COST;
        }
        businessTotal[t].isProcess = true;
    }

    public static void init() {
        Scanner cin = new Scanner(System.in);
        N = cin.nextInt();
        M = cin.nextInt();
        T = cin.nextInt();
        R = cin.nextInt();
        P = cin.nextInt();
        D = cin.nextInt();

        sideArray = new int[M][3];
        for (int x = 0; x < M; x++) {
            sideArray[x][0] = cin.nextInt();
            sideArray[x][1] = cin.nextInt();
            sideArray[x][2] = cin.nextInt();
        }

        // 初始化邻接矩阵
        graph = new Passage[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                graph[i][j] = new Passage();
            }
        }
        ArrayList<int[]> sideArray1 = new ArrayList<>();
        for (int i = 0; i < sideArray.length; i++) {
            int start = sideArray[i][0];
            int end = sideArray[i][1];

            if (i == 0)
                sideArray1.add(sideArray[i]);
            else if (sideArray1.get(sideArray1.size() - 1)[0] != start
                    || sideArray1.get(sideArray1.size() - 1)[1] != end) {
                sideArray1.add(sideArray[i]);
            }

            Side side = new Side(i, start, end, sideArray[i][2]);
            sides.add(side);
            graph[start][end].addSide(i);
            graph[end][start].addSide(i);
        }

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                graph[i][j].sidesSort();
            }
        }

        // 初始化节点间的邻接表
        graphList = new ArrayList[N];
        for (int i = 0; i < N; i++) {
            graphList[i] = new ArrayList<>();
        }
        for (int i = 0; i < sideArray1.size(); i++) {
            int start = sideArray1.get(i)[0];
            int end = sideArray1.get(i)[1];
            graphList[start].add(end);
            graphList[end].add(start);
        }

        // 初始化业务
        businessTotal = new Business[R];
        int r_id = 0;
        samePassBusiness = new int[T];

        for (int x = 0; x < T; x++) {
            int startNode = cin.nextInt();
            int endNode = cin.nextInt();
            int routeNum = cin.nextInt();

            // 当光业务的路径数量为 2 时，这两条路径分配的通道必须相同
            if (routeNum == 2) {
                samePassBusiness[x] = -2;
            } else {
                samePassBusiness[x] = -1; // 多挑或单挑
            }

            for (int y = 0; y < routeNum; y++) {
                int level = y;
                // 当光业务的路径数量为 2 时, level 提到最大
                if(routeNum == 2) {
                    level = 105 + y;
                }

                businessTotal[r_id] = new Business(r_id, startNode, endNode, x, routeNum, level);
                r_id += 1;
            }

        }
        // 初始化 hashset
        businessSideHashSet = new HashSet[T];
        for (int i = 0; i < T; i++) {
            businessSideHashSet[i] = new HashSet<>();
        }

        // 初始化前序节点，用于记录通过的节点从而获取路径
        preList = new ArrayList[N];
        for (int i = 0; i < N; i++) {
            preList[i] = new ArrayList<>();
        }

        // 记录起止节点间的所有最短路径
        minTrackWithT = new ArrayList[N][N];
        for (int i = 0; i < R; i++) {
            int start = businessTotal[i].startNode;
            int end = businessTotal[i].endNode;

            if (minTrackWithT[start][end] == null) {
                if (minTrackWithT[end][start] == null) {
                    ArrayList<Track> tracks = getMinPath(start, end);
                    minTrackWithT[start][end] = tracks;
                } else {
                    ArrayList<Track> tracks = minTrackWithT[end][start];
                    ArrayList<Track> tracks1 = new ArrayList<>();
                    for (Track track : tracks) {
                        LinkedList<Integer> list = new LinkedList<>(track.trackList);
                        Collections.reverse(list);
                        tracks1.add(new Track(list));
                    }
                    minTrackWithT[start][end] = tracks1;
                }
            }

            // minTrackWithT[start][end]

            // 每组路径按 轨迹长度升序，按 总边数/轨迹长度 降序
            Collections.sort(minTrackWithT[start][end], new Comparator<Track>() {
                @Override
                public int compare(Track o1, Track o2) {
                    // int num = o1.trackList.size() - o2.trackList.size();
                    // if (num == 0) {
                    // int cost1 = o1.getTrackCost();
                    // int cost2 = o2.getTrackCost();
                    // // return cost2 - cost1;
                    // return cost1 - cost2;
                    // }
                    // return num;

                    int sideNum1 = 0;
                    int sideNum2 = 0;
                    for (int i = 0; i < o1.trackList.size() - 1; i++) {
                        int start = o1.trackList.get(i);
                        int end = o1.trackList.get(i + 1);
                        sideNum1 += graph[start][end].sides.size();
                    }
                    for (int i = 0; i < o2.trackList.size() - 1; i++) {
                        int start = o2.trackList.get(i);
                        int end = o2.trackList.get(i + 1);
                        sideNum2 += graph[start][end].sides.size();
                    }
                    double s1 = sideNum1 * 1.0 / o1.trackList.size();
                    double s2 = sideNum2 * 1.0 / o2.trackList.size();

//                    HashSet<Integer> hs1 = new HashSet<>();
//                    HashSet<Integer> hs2 = new HashSet<>();
//                    for (int i = 0; i < minTrackWithT[startNode1][endNode1].size(); i++) {
//                        Track track = minTrackWithT[startNode1][endNode1].get(i);
//                        for (int j = 0; j < track.trackList.size() - 1; j++) {
//                            int start = track.trackList.get(j);
//                            int end = track.trackList.get(j + 1);
//                            hs1.addAll(graph[start][end].sides);
//                        }
//                    }
//
//                    for (int i = 0; i < minTrackWithT[startNode2][endNode2].size(); i++) {
//                        Track track = minTrackWithT[startNode2][endNode2].get(i);
//                        for (int j = 0; j < track.trackList.size() - 1; j++) {
//                            int start = track.trackList.get(j);
//                            int end = track.trackList.get(j + 1);
//                            hs2.addAll(graph[start][end].sides);
//                        }
//                    }

                    int num = o1.trackList.size() - o2.trackList.size();
                    return num == 0 ? Double.compare(s2, s1) : num;
                }
            });

            // 过滤不是最短路径的tract
            ArrayList<Track> filterMinTractWithT = new ArrayList<>();
            int min_sz = (int) 1e9;
            for (var track : minTrackWithT[start][end]) {
                int cur_sz = track.trackList.size();
                if (cur_sz < min_sz) {
                    min_sz = track.trackList.size();
                    filterMinTractWithT.clear();
                    filterMinTractWithT.add(track);
                } else if (cur_sz == min_sz) {
                    filterMinTractWithT.add(track);
                }
            }
            minTrackWithT[start][end] = filterMinTractWithT;

            opBusinessMinPathNum(i, 1);

        }

        TreeMap<String, ArrayList<Integer>> tm = new TreeMap<>();
        for (int i = 0; i < R; i++) {
            Business business = businessTotal[i];
            if (business.routeNum != 1) {
                continue;
            }
            int max = Math.max(business.startNode, business.endNode);
            int min = Math.min(business.startNode, business.endNode);
            String s = min + " " + max;

            tm.computeIfAbsent(s, k -> new ArrayList<>());
            tm.get(s).add(i);
        }

        /*
         * for(int i = 0;i<T;i++){
         * businessTotal[i].num =
         * tm.get(businessTotal[i].startNode+" "+businessTotal[i].endNode).size();
         * }
         */

        entryList = new ArrayList<>(tm.entrySet());

        entryList.sort(new Comparator<Map.Entry<String, ArrayList<Integer>>>() {
            @Override
            public int compare(Map.Entry<String, ArrayList<Integer>> o1, Map.Entry<String, ArrayList<Integer>> o2) {

                int startNode1 = businessTotal[o1.getValue().get(0)].startNode;
                int startNode2 = businessTotal[o2.getValue().get(0)].startNode;
                int endNode1 = businessTotal[o1.getValue().get(0)].endNode;
                int endNode2 = businessTotal[o2.getValue().get(0)].endNode;

                // int flexibility1 = 0;
                // int flexibility2 = 0;
                // for(int i = 0;i<minTrackWithT[startNode1][endNode1].size();i++){
                // Track track = minTrackWithT[startNode1][endNode1].get(i);
                // flexibility1+= getSidesNum(track) / track.trackList.size();
                // }
                // for(int i = 0;i<minTrackWithT[startNode2][endNode2].size();i++){
                // Track track = minTrackWithT[startNode2][endNode2].get(i);
                // flexibility2+= getSidesNum(track) / track.trackList.size();
                // }

                int num = o2.getValue().size()
                        - o1.getValue().size();
                // int num1 = num == 0 ? flexibility2 -flexibility1 : num;
                int num2 = num == 0 ? minTrackWithT[startNode1][endNode1].get(0).trackList.size()
                        - minTrackWithT[startNode2][endNode2].get(0).trackList.size() : num;
                // int num3 = num2 == 0? flexibility2 -flexibility1 : num2;
                return num2;
            }
        });
    }

    public static void output() {
        System.out.println(addSide.size());
        for (int i = 0; i < addSide.size(); i++) {
            System.out.println(addSide.get(i)[0] + " " + addSide.get(i)[1]);
        }
        for (int i = 0; i < R; i++) {
            businessTotal[i].print();
        }
    }

    public static void printTrack(int start, int end, LinkedList<Integer> track) {
        track.addFirst(end);
        if (start == end)
            return;
        printTrack(start, pre[end], track);
    }
}

class Business {
    int id;
    boolean isProcess;
    int startNode;
    int endNode;
    int business_id; // 路径条数
    int routeNum;
    int level;

    int num;
    int pId = -1;

    int remainD = Main.D;

    long addSideCost;
    long amplifierCost;
    long edgeCost;

    LinkedList<Integer> sideList = new LinkedList<>();
    LinkedList<Integer> amplifierList = new LinkedList<>();

    public Business(int id, int startNode, int endNode, int business_id, int routeNum, int level) {
        this.id = id;
        this.startNode = startNode;
        this.endNode = endNode;
        this.business_id = business_id;
        this.routeNum = routeNum;
        this.level = level;
    }

    public void print() {
        if (pId == -1)
            return;
        System.out.print(pId + " ");
        System.out.print(sideList.size() + " ");
        System.out.print(amplifierList.size() + " ");
        for (int i : sideList) {
            System.out.print(i + " ");
        }
        for (int i = 0; i < amplifierList.size(); i++) {
            if (i == amplifierList.size() - 1)
                System.out.println(amplifierList.get(i));
            else
                System.out.print(amplifierList.get(i) + " ");
        }
    }
}

class Passage {
    // 两点间的所有边
    ArrayList<Integer> sides = new ArrayList<>();
    int remainPNum;
    int startNode;
    int endNode;

    int minDis;

    public void sidesSort() {
        sides.sort((o1, o2) -> {
            int dis1 = Main.sides.get(o1).dis;
            int dis2 = Main.sides.get(o2).dis;
            return Integer.compare(dis1, dis2);
        });
    }

    // 根据通道找到可以通过的边id
    // public int getSideIdWithFreeP(int p) {
    // for (Integer sideId : sides) {
    // if (!Main.sides.get(sideId).isUsed[p]) {
    // return sideId;
    // }
    // }
    // return -1;
    // }

    public int getFreeSideNum(int p) {
        int result = 0;
        for (Integer sideId : sides) {
            if (!Main.sides.get(sideId).isUsed[p]) {
                result++;
            }
        }
        return result;
    }


    // 根据通道和业务找到可以通过的边id
    public int getSideIdWithFreeP(int p, int task) {
        for (Integer sideId : sides) {
            if (!Main.sides.get(sideId).isUsed[p] && !Main.businessSideHashSet[task].contains(sideId)) {
                return sideId;
            }
        }
        return -1;
    }

    public int getMinDis() {
        int min = Integer.MAX_VALUE;
        for (Integer id : sides) {
            if (min > Main.sides.get(id).dis) {
                min = Main.sides.get(id).dis;
            }
        }
        return min;
    }

    public void addSide(Integer sideId) {
        this.sides.add(sideId);
        remainPNum += Main.P;
    }
}

class Side {
    public int id;
    int dis;
    boolean isAdd;
    boolean[] isUsed = new boolean[Main.P];

    int startNode;
    int endNode;

    int remainPNum = Main.P;

    public Side(int id, int startNode, int endNode, int dis) {
        this.id = id;
        this.dis = dis;
        this.startNode = startNode;
        this.endNode = endNode;
    }
}

class Track {
    // 轨迹的节点list
    ArrayList<Integer> trackList = new ArrayList<>();
    int p;
    // 边list
    ArrayList<Integer> sideList = new ArrayList<>();
    int[] needAddSideNum = new int[Main.P];

    public Track() {
    }

    public int getFreeSideNum(int p) {
        int result = 0;
        //sideList = new ArrayList<>();
        for (int j = 0; j < trackList.size() - 1; j++) {
            int start = trackList.get(j);
            int end = trackList.get(j + 1);
            int num = Main.graph[start][end].getFreeSideNum(p);
            //sideList.add(sideId);
            result += num ;
        }
        return result;
    }

    public int getTrackCost() {
        int edgeCost = trackList.size();
        int amplifierCost = 0;
        int sum_dis = 0;
        var start = -1;
        for (var end : trackList) {
            if (start != -1) {
                sum_dis += Main.graph[start][end].minDis;
                if (sum_dis > Main.D) {
                    amplifierCost += Main.AMPLIFIER_COST;
                    sum_dis = Main.graph[start][end].minDis;
                }
            }
            start = end;
        }
        return edgeCost + amplifierCost;
    }

    public Track(List<Integer> trackList) {
        this.trackList = new ArrayList<>(trackList);
    }

    // 根据trackList和p获取sideList
    public ArrayList<Integer> getSideList(int business_id) {
        sideList = new ArrayList<>();
        for (int i = 0; i < trackList.size() - 1; i++) {
            int start = trackList.get(i);
            int end = trackList.get(i + 1);

            int sideId = Main.graph[start][end].getSideIdWithFreeP(p, business_id);
            sideList.add(sideId);
        }
        return sideList;
    }

    // 当前trackList需要增加的边数
    public int getNeedAddSideNum(int business_id) {
        int needAddSideNum = 0;
        // sideList = new ArrayList<>();
        for (int j = 0; j < trackList.size() - 1; j++) {
            int start = trackList.get(j);
            int end = trackList.get(j + 1);
            int sideId = Main.graph[start][end].getSideIdWithFreeP(p, business_id);
            // sideList.add(sideId);
            needAddSideNum += sideId == -1 ? 1 : 0;
        }
        this.needAddSideNum[p] = needAddSideNum;
        return this.needAddSideNum[p];
    }

    // 当前轨迹和通道合法
    // public boolean isValid() {
    // for (int i = 0; i < trackList.size() - 1; i++) {
    // int start = trackList.get(i);
    // int end = trackList.get(i + 1);

    // int sideId = Main.graph[start][end].getSideIdWithFreeP(p);
    // if (sideId == -1)
    // return false;
    // }
    // return true;
    // }

}