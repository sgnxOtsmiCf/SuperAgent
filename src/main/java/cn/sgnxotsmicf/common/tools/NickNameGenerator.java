package cn.sgnxotsmicf.common.tools;

import cn.hutool.core.util.RandomUtil;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/20 17:39
 * @Version: 1.0
 * @Description:
 */

public class NickNameGenerator {

    private static final List<String> NET_NAMES = Arrays.asList(
            "星眠", "晚雾", "知夏", "沐禾", "屿风", "云兮", "月昭", "清欢",
            "星冉", "夏柠", "枕雪", "听风", "南栀", "北辰", "汐月", "柚茶",
            "安夏", "慕晴", "希颜", "念汐", "禾晚", "星雾", "屿森", "清越",
            "晚星", "柚屿", "晴栀", "予安", "沐晴", "星辞", "风眠", "云舒"
    );

    /**
     * 生成随机中文昵称
     * @return 随机昵称
     */
    public static String generateChineseNickname() {
        return RandomUtil.randomEle(NET_NAMES);
    }

    /**
     * 生成带随机数字后缀的昵称（降低重复概率）
     * @return 如：星眠_8848
     */
    public static String generateChineseNicknameWithSuffix() {
        String name = RandomUtil.randomEle(NET_NAMES);
        int suffix = RandomUtil.randomInt(1000, 9999);
        return name + "_" + suffix;
    }

    /**
     * 生成组合昵称
     * @return 如：星眠_晚雾
     */
    public static String generateCombinedNickname() {
        String first = RandomUtil.randomEle(NET_NAMES);
        String second = RandomUtil.randomEle(NET_NAMES);
        // 避免重复
        while (first.equals(second)) {
            second = RandomUtil.randomEle(NET_NAMES);
        }
        return first + "_" + second;
    }

}
