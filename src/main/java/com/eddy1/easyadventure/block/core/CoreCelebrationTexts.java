package com.eddy1.easyadventure.block.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.Random;

public final class CoreCelebrationTexts {
    private static final List<String> TITLES = List.of(
            "&6&l✦ 基地部署完成 ✦",
            "&b&l✦ SYSTEM ONLINE ✦",
            "&d&l✦ 此心安处 ✦",
            "&a&l✦ 安全屋已着陆 ✦",
            "&5&l✦ 维度投影稳定 ✦",
            "&e&l✦ 欢迎回家 ✦",
            "&3&l✦ 空间折叠完成 ✦"
    );

    private static final List<String> QUOTES = List.of(
            "&b何处是吾乡？&6此心安处是吾乡。",
            "&e既是起点，亦是归途。",
            "&d万水千山，&f不忘来路。",
            "&a风起之处，便是征程。",
            "&b世界虽大，&f总有一盏灯为你而亮。",
            "&c在此驻足，&6为了更好的出发。",
            "&6长路漫漫，&f幸有归处。",
            "&d遍历山河，终有归家。",
            "&5暂歇羽翼，静待风起。",
            "&b[系统] &f空间折叠矩阵展开完成。",
            "&3量子纠缠态：&b已锁定当前坐标。",
            "&9维度投影稳定，&f欢迎回到主物质位面。",
            "&b这里没有 996，&f只有诗和方块。",
            "&eHome, &6sweet home.",
            "&bJourney before destination.",
            "&d星轨指引归途，&f大地承载梦想。",
            "&6重铸秩序，&f安身立命。",
            "&c在此驻足，&e为了更好的出发。",
            "&3空间锚点校准完毕，&b可以开始躺平了。",
            "&5封印解除，&f固有结界展开。",
            "&a风会停，雨会止，&e但家会一直在。",
            "&b世界很吵，&f但门后的灯光很安静。",
            "&6旅人卸甲，&f篝火已燃。",
            "&d诸事烦扰，&5先回家再说。",
            "&6契约已成，&f此地即为理想乡。",
            "&a拔剑四顾心茫然，&e不如回家睡大觉。",
            "&c前方高能，&f安全屋已着陆。",
            "&d传说开始的地方，&5也会是结束的地方。",
            "&aHey, you. &eYou're finally awake.",
            "&eIt's dangerous to go alone! &aTake this.",
            "&dThe cake is a lie. &fBut home is real.",
            "&6赞美太阳，&f然后进门休息。",
            "&b正在载入地图... &a99%",
            "&a恭喜达成成就：&e有房一族。",
            "&3War... &bWar never changes.",
            "&b[系统] &f住宿模块、炊事模块、摸鱼模块全部上线。",
            "&a你负责远征，&f房子负责等你回来。",
            "&6别慌，&f至少今晚不用住山洞。",
            "&d再大的地图，&f也要有一个可以下线的地方。",
            "&5今天也许很糟，&e但床和箱子都还在。",
            "&c苦力怕已被禁止入内。&7(理论上)",
            "&b深渊凝视着你，&f而你只想回家开箱子。",
            "&a长夜未央，&e先回家补个状态。",
            "&6山高路远，&f归处不远。",
            "&d你可以征服世界，&5也可以先回家吃饭。",
            "&3坐标稳定，区块加载，&b可以开始摆烂。",
            "&a今日份安全感，&f已送达。",
            "&e别看了，&f再看也得回家睡觉。",
            "&b真正的传送术，&f是把家一起带走。",
            "&6铁砧会坏，钻镐会坏，&f但家最好别坏。",
            "&d与其在外流浪，&5不如回来整理箱子。",
            "&a进可远征天下，&e退可原地开饭。",
            "&c高能预警结束，&f现在是休息时间。",
            "&3所有路径最终都会通向一扇门。",
            "&b如果前路太冷，&f那就点亮这一盏灯。",
            "&6草方块、工作台、熔炉、床。&f这就叫文明。",
            "&d欢迎来到全世界最懂你的那间小屋。",
            "&a在外是冒险者，&e回家就是屋主。",
            "&5愿今晚的幻翼，&f找不到这里。",
            "&b你已进入舒适区。",
            "&6箱子不会说话，&f但它们记得你的东西。",
            "&d旅程或许漫长，&f但回家的路不该复杂。",
            "&3主线任务暂停。&b支线任务：回家。",
            "&a忙了一整天，&e总该给自己一盏灯。",
            "&c警告：检测到大量“我先回家一下”的冲动。",
            "&b如果现实不能存档，&f至少基地可以。",
            "&6你不是撤退，&f你是在进行战略性回家。",
            "&d归档完成。&5从今天起，这里就是根据地。",
            "&a这不是房子，&f这是你对混乱世界的反击。",
            "&3欢迎回到那个可以安全整理背包的地方。",
            "&e今日宜：回家，整理，补给，继续出发。",
            "&b再强的 Boss，&f也打不过一张舒服的床。",
            "&6外面是远方，&f里面是生活。",
            "&d故事可以继续，&5先让人歇一会儿。",
            "&a这一刻，&b想去挖矿的念头突然消失了。",
            "&6这是魔法，&f牛顿管不了这个。",
            "&dAT力场展开，&f今晚的苦力怕请止步。",
            "&b第 3 新东京市级别防线，&f已部署到位。",
            "&6不是使徒来袭，&f只是你终于想回家了。",
            "&a少年，去创造属于自己的根据地吧。",
            "&5命运石之门选择了这里作为落点。",
            "&bEl Psy Kongroo. &f总之先把门关上。",
            "&d替身攻击也进不来，&f这屋现在很安全。",
            "&6箭矢会偏，拳头会空，&f但家会接住你。",
            "&a今天不做不良少年，&e先做有房人士。",
            "&b这不是地鸣，&f只是你把家搬过来了。",
            "&c献出心脏之前，&f先把背包整理好。",
            "&6调查兵团辛苦了，&f今晚先回据点休整。",
            "&d愿你的旅途像芙莉莲一样漫长，&f但回家这步路很短。",
            "&a魔法可以慢慢学，&e床先放好。",
            "&5欢迎来到咒术高专宿舍楼。&7(并不是)",
            "&b领域展开：&f绝对安全的小屋。",
            "&6今天不开高达，&f今天开家门。",
            "&a宇宙世纪再乱，&e也要先有个落脚点。",
            "&d英灵可以迟到，&f晚饭不能迟到。",
            "&b圣杯战争太危险，&f还是回家开箱子吧。",
            "&6海贼可以继续当，&f今晚先靠岸。",
            "&a伟大航路很远，&e但回家的坐标很近。",
            "&5木叶飞舞之处，&f火亦生生不息；而这里，灯也不灭。",
            "&b今天不结印，&f今天收纳基地。",
            "&6这不是炼成阵，&f但一样能把家带走。",
            "&d等价交换之后，&f请把舒适感还给自己。",
            "&a孤独摇滚结束，&e回家继续一个人爽玩。",
            "&b虽然社交很难，&f但回家很简单。",
            "&5如果这是你的回合，&f那就发动场地魔法：家。",
            "&6决斗可以明天再打，&f今晚先睡觉。",
            "&d鲁路修可以改变世界，&f但你先改变了住址。",
            "&a王的军势很大，&e但这间屋子也足够安心。",
            "&b这不是波纹，也不是替身，&f这是纯粹的归属感。",
            "&6下次出门前记得带上便当，&f和你的卷轴。",
            "&b这里不只是房子，&a这是你的老家。"
    );

    private CoreCelebrationTexts() {
    }

    public static Component randomTitle(Random random) {
        return parseLegacyFormatting(TITLES.get(random.nextInt(TITLES.size())));
    }

    public static Component randomSubtitle(Random random) {
        return parseLegacyFormatting(QUOTES.get(random.nextInt(QUOTES.size())));
    }

    private static Component parseLegacyFormatting(String text) {
        MutableComponent result = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if ((current == '&' || current == '\u00a7') && i + 1 < text.length()) {
                ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(i + 1));
                if (formatting != null) {
                    appendStyled(result, buffer, style);
                    style = applyFormatting(style, formatting);
                    i++;
                    continue;
                }
            }
            buffer.append(current);
        }

        appendStyled(result, buffer, style);
        return result;
    }

    private static Style applyFormatting(Style currentStyle, ChatFormatting formatting) {
        if (formatting == ChatFormatting.RESET) {
            return Style.EMPTY;
        }
        if (formatting.isColor()) {
            return Style.EMPTY.applyFormat(formatting);
        }
        return currentStyle.applyFormat(formatting);
    }

    private static void appendStyled(MutableComponent root, StringBuilder buffer, Style style) {
        if (buffer.isEmpty()) {
            return;
        }
        root.append(Component.literal(buffer.toString()).setStyle(style));
        buffer.setLength(0);
    }
}
