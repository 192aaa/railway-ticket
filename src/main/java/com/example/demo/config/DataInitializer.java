package com.example.demo.config;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final StationRepository stationRepository;
    private final TrainRepository trainRepository;
    private final TrainStationRepository trainStationRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(StationRepository stationRepository,
                           TrainRepository trainRepository,
                           TrainStationRepository trainStationRepository,
                           TicketRepository ticketRepository,
                           UserRepository userRepository,
                           OrderRepository orderRepository,
                           PasswordEncoder passwordEncoder) {
        this.stationRepository = stationRepository;
        this.trainRepository = trainRepository;
        this.trainStationRepository = trainStationRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // ========== 1. 站点扩充 16 -> 35 ==========
        Station bj   = getOrCreateStation("北京",   "BJP");
        Station sh   = getOrCreateStation("上海",   "SHH");
        Station gz   = getOrCreateStation("广州",   "GZQ");
        Station sz   = getOrCreateStation("深圳",   "SZQ");
        Station cd   = getOrCreateStation("成都",   "CDW");
        Station xa   = getOrCreateStation("西安",   "XAY");
        Station hz   = getOrCreateStation("杭州",   "HZH");
        Station nj   = getOrCreateStation("南京",   "NJH");
        Station wh   = getOrCreateStation("武汉",   "WHN");
        Station cs   = getOrCreateStation("长沙",   "CSQ");
        Station cq   = getOrCreateStation("重庆",   "CQW");
        Station tj   = getOrCreateStation("天津",   "TJP");
        Station su   = getOrCreateStation("苏州",   "SZH");
        Station zz   = getOrCreateStation("郑州",   "ZZF");
        Station qd   = getOrCreateStation("青岛",   "QDK");
        Station xm   = getOrCreateStation("厦门",   "XMS");

        Station hhht = getOrCreateStation("哈尔滨", "HBB");
        Station sy   = getOrCreateStation("沈阳",   "SYT");
        Station dl   = getOrCreateStation("大连",   "DLT");
        Station jn   = getOrCreateStation("济南",   "JNK");
        Station sjz  = getOrCreateStation("石家庄", "SJP");
        Station ty   = getOrCreateStation("太原",   "TYV");
        Station hoh  = getOrCreateStation("呼和浩特", "HHC");
        Station lan  = getOrCreateStation("兰州",   "LZJ");
        Station ur   = getOrCreateStation("乌鲁木齐", "WMW");
        Station yn   = getOrCreateStation("银川",   "YIJ");

        Station km   = getOrCreateStation("昆明",   "KMM");
        Station gy   = getOrCreateStation("贵阳",   "GIY");
        Station nn   = getOrCreateStation("南宁",   "NNZ");
        Station gl   = getOrCreateStation("桂林",   "GLZ");

        Station nb   = getOrCreateStation("宁波",   "NBH");
        Station fz   = getOrCreateStation("福州",   "FZS");
        Station hf   = getOrCreateStation("合肥",   "HFH");
        Station nc   = getOrCreateStation("南昌",   "NCG");
        Station ly   = getOrCreateStation("洛阳",   "LYF");
        Station ha   = getOrCreateStation("海口",   "HKQ");
        Station sy2  = getOrCreateStation("三亚",   "SYQ");

        Station allStations[] = {bj, sh, gz, sz, cd, xa, hz, nj, wh, cs, cq, tj, su, zz, qd, xm,
                hhht, sy, dl, jn, sjz, ty, hoh, lan, ur, yn, km, gy, nn, gl, nb, fz, hf, nc, ly, ha, sy2};

        // ========== 2. 车次扩充 50+ -> 130+ ==========

        // --- 京沪线 ---
        saveTrain("G1",   "高铁", bj.getId(), sh.getId(), "08:00", "12:28", 1488, 1748.0, 933.0, 553.0, 0.0);
        saveTrain("G2",   "高铁", bj.getId(), sh.getId(), "10:00", "14:28", 1488, 1748.0, 933.0, 553.0, 0.0);
        saveTrain("G3",   "高铁", bj.getId(), sh.getId(), "14:00", "18:28", 1488, 1748.0, 933.0, 553.0, 0.0);
        saveTrain("G5",   "高铁", bj.getId(), sh.getId(), "16:00", "20:23", 1488, 1748.0, 933.0, 553.0, 0.0);
        saveTrain("G7",   "高铁", bj.getId(), sh.getId(), "20:00", "次日00:30", 1488, 1748.0, 933.0, 553.0, 0.0);
        saveTrain("G101", "高铁", bj.getId(), sh.getId(), "06:20", "11:05", 1488, 1748.0, 933.0, 553.0, 0.0);
        saveTrain("G103", "高铁", bj.getId(), sh.getId(), "07:00", "11:08", 1488, 1748.0, 933.0, 553.0, 0.0);
        saveTrain("G107", "高铁", bj.getId(), sh.getId(), "11:05", "15:44", 1488, 1748.0, 933.0, 553.0, 0.0);
        saveTrain("G109", "高铁", bj.getId(), sh.getId(), "12:20", "16:56", 1488, 1748.0, 933.0, 553.0, 0.0);
        saveTrain("G17",  "高铁", bj.getId(), sh.getId(), "16:00", "20:23", 1488, 1748.0, 933.0, 553.0, 0.0);
        saveTrain("Z9",   "直达", bj.getId(), sh.getId(), "19:28", "09:29", 1463, 0.0, 0.0, 0.0, 281.0);
        saveTrain("Z13",  "直达", bj.getId(), sh.getId(), "20:00", "09:35", 1463, 0.0, 0.0, 0.0, 281.0);
        saveTrain("Z10",  "直达", bj.getId(), sh.getId(), "20:02", "09:22", 1463, 0.0, 0.0, 0.0, 281.0);
        saveTrain("K101", "快速", bj.getId(), sh.getId(), "23:16", "次日15:38", 1550, 0.0, 0.0, 0.0, 281.0);

        // --- 京广线 ---
        saveTrain("G65",  "高铁", bj.getId(), gz.getId(), "10:33", "20:20", 2123, 2464.0, 1333.0, 789.0, 0.0);
        saveTrain("G79",  "高铁", bj.getId(), gz.getId(), "14:00", "22:48", 2298, 2862.0, 1489.0, 862.0, 0.0);
        saveTrain("G81",  "高铁", bj.getId(), gz.getId(), "08:00", "17:50", 2298, 2862.0, 1489.0, 862.0, 0.0);
        saveTrain("G83",  "高铁", bj.getId(), gz.getId(), "12:00", "21:50", 2298, 2862.0, 1489.0, 862.0, 0.0);

        // --- 京深线 ---
        saveTrain("G279", "高铁", bj.getId(), sz.getId(), "07:30", "19:00", 2400, 2794.0, 1392.0, 820.0, 0.0);
        saveTrain("G29",  "高铁", bj.getId(), sz.getId(), "11:00", "22:30", 2400, 2794.0, 1392.0, 820.0, 0.0);
        saveTrain("G31",  "高铁", bj.getId(), sz.getId(), "15:00", "次日02:00", 2400, 2794.0, 1392.0, 820.0, 0.0);

        // --- 京沪沪杭 ---
        saveTrain("G7578", "高铁", sh.getId(), hz.getId(), "08:00", "09:45", 180, 345.0, 173.0, 73.0, 0.0);
        saveTrain("G7582", "高铁", sh.getId(), hz.getId(), "09:30", "11:15", 180, 345.0, 173.0, 73.0, 0.0);
        saveTrain("G7586", "高铁", sh.getId(), hz.getId(), "14:00", "15:45", 180, 345.0, 173.0, 73.0, 0.0);
        saveTrain("G7590", "高铁", sh.getId(), hz.getId(), "18:00", "19:45", 180, 345.0, 173.0, 73.0, 0.0);
        saveTrain("G7592", "高铁", sh.getId(), hz.getId(), "20:00", "21:30", 180, 345.0, 173.0, 73.0, 0.0);

        saveTrain("G7006", "高铁", sh.getId(), nj.getId(), "08:00", "09:30", 301, 475.0, 247.0, 146.0, 0.0);
        saveTrain("G7010", "高铁", sh.getId(), nj.getId(), "10:00", "11:30", 301, 475.0, 247.0, 146.0, 0.0);
        saveTrain("G7014", "高铁", sh.getId(), nj.getId(), "14:00", "15:30", 301, 475.0, 247.0, 146.0, 0.0);
        saveTrain("G7018", "高铁", sh.getId(), nj.getId(), "18:00", "19:30", 301, 475.0, 247.0, 146.0, 0.0);

        saveTrain("G7372", "高铁", sh.getId(), nb.getId(), "07:00", "09:30", 350, 530.0, 275.0, 162.0, 0.0);
        saveTrain("G7376", "高铁", sh.getId(), nb.getId(), "13:00", "15:30", 350, 530.0, 275.0, 162.0, 0.0);

        saveTrain("D3102", "动车", sh.getId(), fz.getId(), "07:30", "15:30", 800, 0.0, 0.0, 260.0, 230.0);
        saveTrain("D3104", "动车", sh.getId(), xm.getId(), "09:00", "18:00", 1000, 0.0, 0.0, 320.0, 280.0);

        // --- 沪广深 ---
        saveTrain("G305",  "高铁", sh.getId(), gz.getId(), "07:42", "15:20", 1790, 2093.0, 1133.0, 662.0, 0.0);
        saveTrain("G85",   "高铁", sh.getId(), gz.getId(), "08:00", "16:00", 1790, 2093.0, 1133.0, 662.0, 0.0);
        saveTrain("G1301", "高铁", sh.getId(), sz.getId(), "09:30", "17:45", 1800, 2024.0, 1080.0, 637.0, 0.0);
        saveTrain("G1311", "高铁", sh.getId(), sz.getId(), "12:05", "20:30", 1800, 2024.0, 1080.0, 637.0, 0.0);
        saveTrain("G1315", "高铁", sh.getId(), sz.getId(), "15:00", "23:10", 1800, 2024.0, 1080.0, 637.0, 0.0);
        saveTrain("Z99",   "直达", sh.getId(), gz.getId(), "18:00", "次日10:00", 1810, 0.0, 0.0, 0.0, 470.0);

        // --- 武汉枢纽 ---
        saveTrain("G1001", "高铁", wh.getId(), sz.getId(), "09:00", "14:30", 1170, 1362.0, 735.0, 432.0, 0.0);
        saveTrain("G1005", "高铁", wh.getId(), sz.getId(), "11:00", "16:40", 1170, 1362.0, 735.0, 432.0, 0.0);
        saveTrain("G1011", "高铁", wh.getId(), gz.getId(), "07:30", "11:40", 1070, 1200.0, 640.0, 380.0, 0.0);
        saveTrain("G1015", "高铁", wh.getId(), gz.getId(), "13:00", "17:20", 1070, 1200.0, 640.0, 380.0, 0.0);
        saveTrain("G1021", "高铁", wh.getId(), nj.getId(), "08:00", "12:10", 600, 620.0, 330.0, 200.0, 0.0);
        saveTrain("G208",  "高铁", wh.getId(), bj.getId(), "07:00", "12:15", 1225, 1450.0, 780.0, 470.0, 0.0);
        saveTrain("G316",  "高铁", wh.getId(), cd.getId(), "09:00", "18:30", 1100, 1350.0, 730.0, 440.0, 0.0);

        // --- 成渝线 ---
        saveTrain("G431",  "高铁", cd.getId(), bj.getId(), "07:30", "19:35", 1875, 2300.0, 1230.0, 750.0, 0.0);
        saveTrain("G90",   "高铁", cd.getId(), bj.getId(), "09:30", "21:10", 1875, 2300.0, 1230.0, 750.0, 0.0);
        saveTrain("G2883", "高铁", cd.getId(), gz.getId(), "08:00", "16:10", 1200, 1500.0, 810.0, 490.0, 0.0);
        saveTrain("G2885", "高铁", cd.getId(), sz.getId(), "10:00", "18:30", 1400, 1680.0, 900.0, 550.0, 0.0);
        saveTrain("G2891", "高铁", cd.getId(), hz.getId(), "07:00", "19:50", 1800, 2000.0, 1120.0, 660.0, 0.0);
        saveTrain("D2201", "动车", cd.getId(), cq.getId(), "08:00", "10:30", 340, 0.0, 0.0, 120.0, 100.0);
        saveTrain("D2205", "动车", cd.getId(), cq.getId(), "14:00", "16:30", 340, 0.0, 0.0, 120.0, 100.0);
        saveTrain("D2207", "动车", cd.getId(), cq.getId(), "18:00", "20:30", 340, 0.0, 0.0, 120.0, 100.0);
        saveTrain("Z50",   "直达", cd.getId(), sh.getId(), "11:43", "次日10:00", 2150, 0.0, 0.0, 0.0, 470.0);
        saveTrain("K117",  "快速", bj.getId(), cd.getId(), "11:10", "次日21:57", 2096, 0.0, 0.0, 0.0, 365.0);
        saveTrain("K817",  "快速", bj.getId(), cq.getId(), "14:10", "次日22:00", 2070, 0.0, 0.0, 0.0, 358.0);

        // --- 重庆出发 ---
        saveTrain("G401",  "高铁", cq.getId(), bj.getId(), "07:50", "19:00", 1750, 2000.0, 1080.0, 650.0, 0.0);
        saveTrain("G1756", "高铁", cq.getId(), sh.getId(), "09:00", "19:40", 1500, 1800.0, 972.0, 583.0, 0.0);
        saveTrain("G2240", "高铁", cq.getId(), gz.getId(), "10:30", "17:30", 1100, 1320.0, 712.0, 430.0, 0.0);
        saveTrain("D658",  "动车", cq.getId(), xm.getId(), "07:00", "19:30", 1400, 0.0, 0.0, 460.0, 400.0);

        // --- 西安枢纽 ---
        saveTrain("D1",    "动车", bj.getId(), xa.getId(), "07:00", "12:42", 1216, 0.0, 0.0, 263.0, 250.0);
        saveTrain("D3",    "动车", bj.getId(), xa.getId(), "09:00", "14:50", 1216, 0.0, 0.0, 263.0, 250.0);
        saveTrain("G87",   "高铁", xa.getId(), bj.getId(), "08:00", "12:35", 1216, 1400.0, 736.0, 418.0, 0.0);
        saveTrain("G97",   "高铁", xa.getId(), gz.getId(), "09:30", "17:48", 1950, 2250.0, 1175.0, 720.0, 0.0);
        saveTrain("G1970", "高铁", xa.getId(), sh.getId(), "07:00", "18:00", 1500, 1750.0, 945.0, 560.0, 0.0);
        saveTrain("G1898", "高铁", xa.getId(), hz.getId(), "08:30", "21:00", 1600, 1900.0, 1026.0, 617.0, 0.0);
        saveTrain("Z20",   "直达", xa.getId(), sh.getId(), "19:30", "次日08:00", 1509, 0.0, 0.0, 0.0, 330.0);
        saveTrain("K1318", "快速", xa.getId(), cd.getId(), "21:22", "次日13:00", 842, 0.0, 0.0, 0.0, 120.0);

        // --- 杭州/宁波 ---
        saveTrain("G202",  "高铁", hz.getId(), bj.getId(), "08:00", "12:20", 1500, 1800.0, 972.0, 583.0, 0.0);
        saveTrain("G32",   "高铁", hz.getId(), bj.getId(), "09:30", "14:00", 1500, 1800.0, 972.0, 583.0, 0.0);
        saveTrain("G7332", "高铁", hz.getId(), xm.getId(), "07:00", "11:30", 800, 960.0, 518.0, 310.0, 0.0);
        saveTrain("G7342", "高铁", hz.getId(), fz.getId(), "08:30", "13:00", 750, 900.0, 486.0, 292.0, 0.0);
        saveTrain("G7502", "高铁", nb.getId(), sh.getId(), "07:00", "09:30", 350, 530.0, 275.0, 162.0, 0.0);
        saveTrain("G7504", "高铁", nb.getId(), hz.getId(), "08:30", "10:00", 150, 230.0, 120.0, 72.0, 0.0);

        // --- 南京/苏州/合肥 ---
        saveTrain("G100",  "高铁", nj.getId(), bj.getId(), "07:00", "10:40", 1024, 1200.0, 650.0, 395.0, 0.0);
        saveTrain("G102",  "高铁", nj.getId(), bj.getId(), "10:00", "13:40", 1024, 1200.0, 650.0, 395.0, 0.0);
        saveTrain("G104",  "高铁", nj.getId(), bj.getId(), "14:00", "17:40", 1024, 1200.0, 650.0, 395.0, 0.0);
        saveTrain("G106",  "高铁", nj.getId(), bj.getId(), "18:00", "21:40", 1024, 1200.0, 650.0, 395.0, 0.0);
        saveTrain("G7088", "高铁", nj.getId(), su.getId(), "08:00", "08:25", 85, 130.0, 70.0, 42.0, 0.0);
        saveTrain("G7090", "高铁", su.getId(), nj.getId(), "09:00", "09:25", 85, 130.0, 70.0, 42.0, 0.0);
        saveTrain("G7571", "高铁", sh.getId(), su.getId(), "07:00", "07:30", 84, 120.0, 65.0, 38.0, 0.0);
        saveTrain("G7573", "高铁", sh.getId(), su.getId(), "12:00", "12:30", 84, 120.0, 65.0, 38.0, 0.0);
        saveTrain("G7575", "高铁", sh.getId(), su.getId(), "17:00", "17:30", 84, 120.0, 65.0, 38.0, 0.0);
        saveTrain("G7577", "高铁", sh.getId(), su.getId(), "20:00", "20:30", 84, 120.0, 65.0, 38.0, 0.0);
        saveTrain("G7579", "高铁", su.getId(), sh.getId(), "08:00", "08:30", 84, 120.0, 65.0, 38.0, 0.0);
        saveTrain("G7581", "高铁", su.getId(), sh.getId(), "14:00", "14:30", 84, 120.0, 65.0, 38.0, 0.0);
        saveTrain("G7583", "高铁", su.getId(), hz.getId(), "09:00", "10:15", 200, 305.0, 158.0, 94.0, 0.0);
        saveTrain("G7585", "高铁", su.getId(), hz.getId(), "16:00", "17:15", 200, 305.0, 158.0, 94.0, 0.0);

        saveTrain("G264",  "高铁", hf.getId(), bj.getId(), "07:00", "10:15", 950, 1140.0, 615.0, 380.0, 0.0);
        saveTrain("G7594", "高铁", hf.getId(), sh.getId(), "08:00", "11:00", 610, 720.0, 390.0, 240.0, 0.0);

        // --- 郑州 ---
        saveTrain("G205",  "高铁", zz.getId(), bj.getId(), "07:10", "09:30", 700, 900.0, 480.0, 295.0, 0.0);
        saveTrain("G206",  "高铁", zz.getId(), bj.getId(), "11:00", "13:25", 700, 900.0, 480.0, 295.0, 0.0);
        saveTrain("G2021", "高铁", zz.getId(), bj.getId(), "14:00", "16:30", 700, 900.0, 480.0, 295.0, 0.0);
        saveTrain("G2023", "高铁", zz.getId(), bj.getId(), "17:00", "19:30", 700, 900.0, 480.0, 295.0, 0.0);
        saveTrain("G94",   "高铁", zz.getId(), sh.getId(), "09:00", "14:30", 980, 1160.0, 625.0, 380.0, 0.0);
        saveTrain("G1532", "高铁", zz.getId(), gz.getId(), "11:00", "18:40", 1500, 1750.0, 945.0, 570.0, 0.0);
        saveTrain("Z54",   "直达", zz.getId(), sh.getId(), "23:00", "次日11:30", 990, 0.0, 0.0, 0.0, 220.0);

        // --- 青岛 ---
        saveTrain("G180",  "高铁", qd.getId(), bj.getId(), "07:00", "11:45", 800, 975.0, 525.0, 320.0, 0.0);
        saveTrain("G182",  "高铁", qd.getId(), bj.getId(), "10:00", "14:40", 800, 975.0, 525.0, 320.0, 0.0);
        saveTrain("G184",  "高铁", qd.getId(), bj.getId(), "14:00", "18:40", 800, 975.0, 525.0, 320.0, 0.0);
        saveTrain("G278",  "高铁", qd.getId(), sh.getId(), "08:30", "14:30", 1000, 1200.0, 648.0, 392.0, 0.0);
        saveTrain("G280",  "高铁", qd.getId(), sh.getId(), "13:00", "19:00", 1000, 1200.0, 648.0, 392.0, 0.0);

        // --- 天津 ---
        saveTrain("G120",  "高铁", tj.getId(), sh.getId(), "08:00", "12:30", 1400, 1600.0, 864.0, 518.0, 0.0);
        saveTrain("G122",  "高铁", tj.getId(), sh.getId(), "11:00", "15:30", 1400, 1600.0, 864.0, 518.0, 0.0);
        saveTrain("G124",  "高铁", tj.getId(), bj.getId(), "09:00", "10:30", 120, 210.0, 112.0, 66.0, 0.0);
        saveTrain("G126",  "高铁", tj.getId(), bj.getId(), "14:00", "15:30", 120, 210.0, 112.0, 66.0, 0.0);
        saveTrain("G128",  "高铁", tj.getId(), bj.getId(), "18:00", "19:30", 120, 210.0, 112.0, 66.0, 0.0);

        // --- 长沙 ---
        saveTrain("G1009", "高铁", cs.getId(), bj.getId(), "09:00", "16:10", 1600, 1900.0, 1026.0, 617.0, 0.0);
        saveTrain("G1013", "高铁", cs.getId(), gz.getId(), "08:00", "12:00", 700, 840.0, 450.0, 270.0, 0.0);
        saveTrain("G1017", "高铁", cs.getId(), gz.getId(), "14:00", "18:00", 700, 840.0, 450.0, 270.0, 0.0);
        saveTrain("G1025", "高铁", cs.getId(), sh.getId(), "07:30", "13:50", 980, 1180.0, 637.0, 380.0, 0.0);
        saveTrain("G1044", "高铁", cs.getId(), cd.getId(), "10:00", "20:00", 1100, 1350.0, 730.0, 440.0, 0.0);

        // --- 东北线 ---
        saveTrain("G902",  "高铁", bj.getId(), sy.getId(), "07:00", "11:00", 690, 880.0, 470.0, 290.0, 0.0);
        saveTrain("G904",  "高铁", bj.getId(), sy.getId(), "10:00", "14:00", 690, 880.0, 470.0, 290.0, 0.0);
        saveTrain("G906",  "高铁", bj.getId(), sy.getId(), "14:00", "18:00", 690, 880.0, 470.0, 290.0, 0.0);
        saveTrain("G908",  "高铁", bj.getId(), hhht.getId(), "08:00", "13:30", 1240, 1460.0, 788.0, 476.0, 0.0);
        saveTrain("G910",  "高铁", bj.getId(), hhht.getId(), "15:00", "20:30", 1240, 1460.0, 788.0, 476.0, 0.0);
        saveTrain("G1226", "高铁", sy.getId(), hhht.getId(), "09:00", "14:00", 700, 900.0, 486.0, 294.0, 0.0);
        saveTrain("G796",  "高铁", dl.getId(), bj.getId(), "08:00", "12:00", 800, 980.0, 529.0, 322.0, 0.0);
        saveTrain("G800",  "高铁", dl.getId(), bj.getId(), "14:00", "18:00", 800, 980.0, 529.0, 322.0, 0.0);
        saveTrain("D407",  "动车", sy.getId(), dl.getId(), "07:30", "11:00", 397, 0.0, 0.0, 180.0, 160.0);
        saveTrain("D409",  "动车", sy.getId(), dl.getId(), "13:00", "16:30", 397, 0.0, 0.0, 180.0, 160.0);
        saveTrain("Z114",  "直达", hhht.getId(), sh.getId(), "09:00", "次日09:00", 1800, 0.0, 0.0, 0.0, 420.0);

        // --- 华北/华东 ---
        saveTrain("G2068", "高铁", jn.getId(), bj.getId(), "07:00", "09:30", 426, 580.0, 310.0, 195.0, 0.0);
        saveTrain("G2070", "高铁", jn.getId(), bj.getId(), "10:00", "12:30", 426, 580.0, 310.0, 195.0, 0.0);
        saveTrain("G226",  "高铁", jn.getId(), sh.getId(), "07:30", "11:30", 880, 1080.0, 586.0, 355.0, 0.0);
        saveTrain("G178",  "高铁", qd.getId(), jn.getId(), "08:00", "11:00", 390, 540.0, 290.0, 180.0, 0.0);
        saveTrain("G688",  "高铁", sjz.getId(), bj.getId(), "07:00", "08:30", 281, 420.0, 225.0, 140.0, 0.0);
        saveTrain("G690",  "高铁", sjz.getId(), bj.getId(), "10:00", "11:30", 281, 420.0, 225.0, 140.0, 0.0);
        saveTrain("G692",  "高铁", sjz.getId(), sh.getId(), "13:00", "19:00", 1100, 1320.0, 713.0, 432.0, 0.0);
        saveTrain("G602",  "高铁", ty.getId(), bj.getId(), "07:00", "09:20", 513, 680.0, 368.0, 225.0, 0.0);
        saveTrain("G604",  "高铁", ty.getId(), bj.getId(), "10:00", "12:20", 513, 680.0, 368.0, 225.0, 0.0);
        saveTrain("G614",  "高铁", ty.getId(), sh.getId(), "15:00", "21:30", 1300, 1560.0, 842.0, 510.0, 0.0);

        // --- 西北 ---
        saveTrain("G464",  "高铁", lan.getId(), bj.getId(), "07:30", "15:40", 1800, 2150.0, 1160.0, 690.0, 0.0);
        saveTrain("G466",  "高铁", lan.getId(), bj.getId(), "12:00", "20:00", 1800, 2150.0, 1160.0, 690.0, 0.0);
        saveTrain("G834",  "高铁", lan.getId(), xa.getId(), "08:00", "12:30", 630, 780.0, 420.0, 255.0, 0.0);
        saveTrain("G836",  "高铁", lan.getId(), xa.getId(), "14:00", "18:30", 630, 780.0, 420.0, 255.0, 0.0);
        saveTrain("Z56",   "直达", lan.getId(), sh.getId(), "13:00", "次日14:30", 2100, 0.0, 0.0, 0.0, 500.0);
        saveTrain("K596",  "快速", ur.getId(), sh.getId(), "11:30", "次日00:30", 3968, 0.0, 0.0, 0.0, 720.0);
        saveTrain("K1584", "快速", ur.getId(), cd.getId(), "16:00", "次日08:30", 3000, 0.0, 0.0, 0.0, 520.0);
        saveTrain("K1178", "快速", yn.getId(), bj.getId(), "07:00", "次日13:00", 1700, 0.0, 0.0, 0.0, 310.0);
        saveTrain("G1876", "高铁", hoh.getId(), cd.getId(), "07:00", "21:30", 2100, 2550.0, 1375.0, 815.0, 0.0);
        saveTrain("Z179",  "直达", hoh.getId(), gz.getId(), "15:00", "次日18:00", 2400, 0.0, 0.0, 0.0, 560.0);

        // --- 西南 ---
        saveTrain("G402",  "高铁", km.getId(), bj.getId(), "07:00", "22:30", 2760, 3200.0, 1720.0, 1030.0, 0.0);
        saveTrain("G286",  "高铁", km.getId(), sh.getId(), "08:00", "22:00", 2260, 2600.0, 1400.0, 840.0, 0.0);
        saveTrain("G2863", "高铁", cd.getId(), km.getId(), "09:00", "15:30", 1100, 1300.0, 702.0, 423.0, 0.0);
        saveTrain("G2865", "高铁", cd.getId(), km.getId(), "14:00", "20:30", 1100, 1300.0, 702.0, 423.0, 0.0);
        saveTrain("G2849", "高铁", gy.getId(), bj.getId(), "10:00", "21:30", 1900, 2300.0, 1234.0, 740.0, 0.0);
        saveTrain("G2853", "高铁", gy.getId(), sh.getId(), "12:00", "22:00", 1500, 1800.0, 972.0, 583.0, 0.0);
        saveTrain("K841",  "快速", gl.getId(), cd.getId(), "13:00", "次日08:30", 1200, 0.0, 0.0, 0.0, 210.0);
        saveTrain("K588",  "快速", gl.getId(), gz.getId(), "16:00", "次日05:00", 883, 0.0, 0.0, 0.0, 140.0);
        saveTrain("G422",  "高铁", nn.getId(), bj.getId(), "08:00", "20:00", 2200, 2600.0, 1400.0, 840.0, 0.0);
        saveTrain("G1502", "高铁", nn.getId(), gz.getId(), "09:00", "13:00", 670, 800.0, 432.0, 260.0, 0.0);

        // --- 华南 ---
        saveTrain("D7301", "动车", gz.getId(), sz.getId(), "07:00", "08:30", 140, 0.0, 0.0, 80.0, 70.0);
        saveTrain("D7303", "动车", gz.getId(), sz.getId(), "09:00", "10:30", 140, 0.0, 0.0, 80.0, 70.0);
        saveTrain("D7305", "动车", gz.getId(), sz.getId(), "11:00", "12:30", 140, 0.0, 0.0, 80.0, 70.0);
        saveTrain("D7307", "动车", gz.getId(), sz.getId(), "13:00", "14:30", 140, 0.0, 0.0, 80.0, 70.0);
        saveTrain("D7309", "动车", gz.getId(), sz.getId(), "15:00", "16:30", 140, 0.0, 0.0, 80.0, 70.0);
        saveTrain("D7311", "动车", gz.getId(), sz.getId(), "17:00", "18:30", 140, 0.0, 0.0, 80.0, 70.0);
        saveTrain("D7313", "动车", gz.getId(), sz.getId(), "19:00", "20:30", 140, 0.0, 0.0, 80.0, 70.0);

        saveTrain("G291",  "高铁", gz.getId(), ha.getId(), "08:00", "12:30", 800, 950.0, 513.0, 309.0, 0.0);
        saveTrain("G293",  "高铁", gz.getId(), ha.getId(), "14:00", "18:30", 800, 950.0, 513.0, 309.0, 0.0);
        saveTrain("D7101", "动车", ha.getId(), sy2.getId(), "09:00", "12:30", 350, 0.0, 0.0, 180.0, 158.0);
        saveTrain("D7103", "动车", ha.getId(), sy2.getId(), "14:00", "17:30", 350, 0.0, 0.0, 180.0, 158.0);

        // --- 福州/厦门 ---
        saveTrain("D6505", "动车", fz.getId(), xm.getId(), "08:00", "09:45", 276, 0.0, 0.0, 110.0, 98.0);
        saveTrain("D6507", "动车", fz.getId(), xm.getId(), "10:00", "11:45", 276, 0.0, 0.0, 110.0, 98.0);
        saveTrain("D6509", "动车", fz.getId(), xm.getId(), "14:00", "15:45", 276, 0.0, 0.0, 110.0, 98.0);
        saveTrain("D6511", "动车", fz.getId(), xm.getId(), "18:00", "19:45", 276, 0.0, 0.0, 110.0, 98.0);
        saveTrain("G1680", "高铁", xm.getId(), sz.getId(), "08:00", "11:40", 650, 780.0, 420.0, 253.0, 0.0);
        saveTrain("G1682", "高铁", xm.getId(), gz.getId(), "13:00", "17:00", 800, 960.0, 518.0, 310.0, 0.0);

        // --- 南昌 ---
        saveTrain("G1460", "高铁", nc.getId(), bj.getId(), "07:00", "14:30", 1500, 1800.0, 972.0, 583.0, 0.0);
        saveTrain("G1462", "高铁", nc.getId(), sh.getId(), "09:00", "14:30", 980, 1200.0, 648.0, 392.0, 0.0);
        saveTrain("G1464", "高铁", nc.getId(), gz.getId(), "11:00", "16:00", 800, 960.0, 518.0, 310.0, 0.0);
        saveTrain("G1466", "高铁", nc.getId(), sz.getId(), "13:00", "18:30", 900, 1080.0, 583.0, 350.0, 0.0);

        // --- 洛阳 ---
        saveTrain("G2004", "高铁", ly.getId(), bj.getId(), "07:00", "10:00", 800, 980.0, 529.0, 322.0, 0.0);
        saveTrain("G2006", "高铁", ly.getId(), sh.getId(), "12:00", "18:30", 1100, 1320.0, 713.0, 432.0, 0.0);
        saveTrain("G2008", "高铁", ly.getId(), gz.getId(), "14:00", "21:30", 1500, 1750.0, 945.0, 570.0, 0.0);

        // ========== 3. 余票初始化 ==========
        LocalDate today = LocalDate.now();
        LocalDate weekLater = today.plusDays(6);
        long futureTicketCount = ticketRepository.countByTrainDateBetween(today, weekLater);
        long expectedTicketCount = trainRepository.count() * 7;

        if (futureTicketCount < expectedTicketCount) {
            System.out.println("===== 初始化未来7天余票 (已有 " + futureTicketCount + "/" + expectedTicketCount + ") =====");
            List<Train> allTrains = trainRepository.findAll();
            for (Train train : allTrains) {
                for (int i = 0; i < 7; i++) {
                    LocalDate date = today.plusDays(i);
                    ticketRepository.findByTrainIdAndTrainDate(train.getId(), date).ifPresentOrElse(
                            existing -> {
                                if (existing.getBusinessSeatCount() == 0 && existing.getFirstSeatCount() == 0
                                        && existing.getSecondSeatCount() == 0 && existing.getSleeperCount() == 0) {
                                    refillTicket(existing, train);
                                    ticketRepository.save(existing);
                                }
                            },
                            () -> {
                                Ticket t = buildTicket(train.getId(), date, 0, 0, 0, 0);
                                refillTicket(t, train);
                                ticketRepository.save(t);
                            }
                    );
                }
            }
        }

        // ========== 4. 测试用户 ==========
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setRealName("管理员");
            admin.setIdCard("110101199001011234");
            admin.setPhone("13800138000");
            userRepository.save(admin);
        }
        if (!userRepository.existsByUsername("zhangsan")) {
            User u = new User();
            u.setUsername("zhangsan");
            u.setPassword(passwordEncoder.encode("123456"));
            u.setRealName("张三");
            u.setIdCard("310101199001011234");
            u.setPhone("13900139001");
            userRepository.save(u);
        }
        if (!userRepository.existsByUsername("lisi")) {
            User u = new User();
            u.setUsername("lisi");
            u.setPassword(passwordEncoder.encode("123456"));
            u.setRealName("李四");
            u.setIdCard("440101199001011234");
            u.setPhone("13900139002");
            userRepository.save(u);
        }
        if (!userRepository.existsByUsername("wangwu")) {
            User u = new User();
            u.setUsername("wangwu");
            u.setPassword(passwordEncoder.encode("123456"));
            u.setRealName("王五");
            u.setIdCard("510101199001011234");
            u.setPhone("13900139003");
            userRepository.save(u);
        }

        // ========== 5. 历史订单 ==========
        if (orderRepository.count() == 0 && trainRepository.count() > 0) {
            System.out.println("===== 生成模拟历史订单 =====");
            generateMockOrders();
        }

        // ========== 6. 汇总 ==========
        System.out.println("==================================================");
        System.out.println("       🚄 铁路购票系统数据初始化完成");
        System.out.println("==================================================");
        System.out.println("  站点数     : " + stationRepository.count() + " 个");
        System.out.println("  车次数     : " + trainRepository.count() + " 趟");
        System.out.println("  未来7天余票: " + ticketRepository.countByTrainDateBetween(today, weekLater) + " 条");
        System.out.println("  用户数     : " + userRepository.count() + " 个");
        System.out.println("  历史订单   : " + orderRepository.count() + " 条");
        System.out.println("--------------------------------------------------");
        System.out.println("  测试账号: admin / zhangsan / lisi / wangwu");
        System.out.println("  密码均为: 123456");
        System.out.println("==================================================");
    }

    private void generateMockOrders() {
        List<Train> trains = trainRepository.findAll();
        List<User> users = userRepository.findAll();
        Random rand = new Random(42);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String[] seatTypes = {"商务座", "一等座", "二等座", "硬卧"};
        String[] statuses = {"PAID", "COMPLETED", "CANCELLED", "PENDING"};

        int orderCount = 0;
        int targetOrders = 50;
        for (int i = 0; i < targetOrders; i++) {
            Train train = trains.get(rand.nextInt(trains.size()));
            User user = users.get(rand.nextInt(users.size()));

            LocalDate trainDate = LocalDate.now().minusDays(rand.nextInt(30));
            LocalDateTime createTime = LocalDateTime.now().minusDays(rand.nextInt(60))
                    .minusHours(rand.nextInt(24))
                    .minusMinutes(rand.nextInt(60));

            String seatType;
            Double price;
            String trainNum = train.getTrainNumber();
            if (trainNum.startsWith("G")) {
                int seatIdx = rand.nextInt(4);
                seatType = seatTypes[seatIdx];
                price = seatIdx == 0 ? train.getBusinessSeatPrice()
                        : seatIdx == 1 ? train.getFirstSeatPrice()
                        : seatIdx == 2 ? train.getSecondSeatPrice()
                        : train.getSleeperPrice();
            } else if (trainNum.startsWith("D")) {
                seatType = rand.nextBoolean() ? "二等座" : "硬卧";
                price = seatType.equals("二等座") ? train.getSecondSeatPrice() : train.getSleeperPrice();
                if (price == null || price == 0) price = seatType.equals("二等座") ? 120.0 : 100.0;
            } else {
                seatType = "硬卧";
                price = train.getSleeperPrice();
                if (price == null || price == 0) price = 200.0;
            }

            int st = rand.nextInt(10);
            String status = st < 5 ? "COMPLETED" : st < 8 ? "PAID" : st == 8 ? "CANCELLED" : "PENDING";

            Order order = new Order();
            order.setOrderNo("T" + System.currentTimeMillis() + String.format("%04d", i));
            order.setUserId(user.getId());
            order.setTrainId(train.getId());
            order.setTrainNumber(train.getTrainNumber());
            order.setTrainDate(trainDate.format(dtf));
            order.setSeatType(seatType);
            order.setSeatNo(String.format("%02d%c", rand.nextInt(20) + 1, (char) ('A' + rand.nextInt(5))));
            order.setPrice(price);
            order.setStatus(status);
            order.setCreateTime(createTime);
            order.setPayTime(status.equals("PENDING") || status.equals("CANCELLED") ? null : createTime.plusMinutes(rand.nextInt(30) + 1));
            orderRepository.save(order);
            orderCount++;
        }
        System.out.println("  已生成 " + orderCount + " 条模拟历史订单");
    }

    private void saveTrain(String number, String type, Long fromId, Long toId,
                           String startTime, String endTime, int duration,
                           Double business, Double first, Double second, Double sleeper) {
        if (trainRepository.existsByTrainNumber(number)) return;
        trainRepository.save(buildTrain(number, type, fromId, toId, startTime, endTime, duration,
                business, first, second, sleeper));
    }

    private void refillTicket(Ticket t, Train train) {
        int type = train.getTrainNumber().startsWith("G") ? 0
                : train.getTrainNumber().startsWith("D") ? 1
                : train.getTrainType().equals("直达") ? 2 : 3;

        switch (type) {
            case 0:
                t.setBusinessSeatCount(10);
                t.setFirstSeatCount(50);
                t.setSecondSeatCount(100);
                t.setSleeperCount(0);
                break;
            case 1:
                t.setBusinessSeatCount(0);
                t.setFirstSeatCount(0);
                t.setSecondSeatCount(120);
                t.setSleeperCount(80);
                break;
            case 2:
                t.setBusinessSeatCount(0);
                t.setFirstSeatCount(0);
                t.setSecondSeatCount(0);
                t.setSleeperCount(50);
                break;
            default:
                t.setBusinessSeatCount(0);
                t.setFirstSeatCount(0);
                t.setSecondSeatCount(0);
                t.setSleeperCount(60);
        }
    }

    private Station buildStation(String name, String city, String code) {
        Station s = new Station();
        s.setName(name);
        s.setCity(city);
        s.setCode(code);
        return s;
    }

    private Station getOrCreateStation(String name, String code) {
        Station s = stationRepository.findByName(name);
        if (s != null) return s;
        return stationRepository.save(buildStation(name, name, code));
    }

    private Train buildTrain(String number, String type, Long fromId, Long toId,
                             String startTime, String endTime, int duration,
                             Double business, Double first, Double second, Double sleeper) {
        Train t = new Train();
        t.setTrainNumber(number);
        t.setTrainType(type);
        t.setFromStationId(fromId);
        t.setToStationId(toId);
        t.setStartTime(startTime);
        t.setEndTime(endTime);
        t.setDuration(duration);
        t.setBusinessSeatPrice(business);
        t.setFirstSeatPrice(first);
        t.setSecondSeatPrice(second);
        t.setSleeperPrice(sleeper);
        return t;
    }

    private Ticket buildTicket(Long trainId, LocalDate date, int business, int first, int second, int sleeper) {
        Ticket t = new Ticket();
        t.setTrainId(trainId);
        t.setTrainDate(date);
        t.setBusinessSeatCount(business);
        t.setFirstSeatCount(first);
        t.setSecondSeatCount(second);
        t.setSleeperCount(sleeper);
        return t;
    }
}