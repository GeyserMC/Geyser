/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.utils;

public enum MapColor {
    COLOR_0(-1, -1, -1),
    COLOR_1(-1, -1, -1),
    COLOR_2(-1, -1, -1),
    COLOR_3(-1, -1, -1),
    COLOR_4(89, 125, 39),
    COLOR_5(109, 153, 48),
    COLOR_6(127, 178, 56),
    COLOR_7(67, 94, 29),
    COLOR_8(174, 164, 115),
    COLOR_9(213, 201, 140),
    COLOR_10(247, 233, 163),
    COLOR_11(130, 123, 86),
    COLOR_12(140, 140, 140),
    COLOR_13(171, 171, 171),
    COLOR_14(199, 199, 199),
    COLOR_15(105, 105, 105),
    COLOR_16(180, 0, 0),
    COLOR_17(220, 0, 0),
    COLOR_18(255, 0, 0),
    COLOR_19(135, 0, 0),
    COLOR_20(112, 112, 180),
    COLOR_21(138, 138, 220),
    COLOR_22(160, 160, 255),
    COLOR_23(84, 84, 135),
    COLOR_24(117, 117, 117),
    COLOR_25(144, 144, 144),
    COLOR_26(167, 167, 167),
    COLOR_27(88, 88, 88),
    COLOR_28(0, 87, 0),
    COLOR_29(0, 106, 0),
    COLOR_30(0, 124, 0),
    COLOR_31(0, 65, 0),
    COLOR_32(180, 180, 180),
    COLOR_33(220, 220, 220),
    COLOR_34(255, 255, 255),
    COLOR_35(135, 135, 135),
    COLOR_36(115, 118, 129),
    COLOR_37(141, 144, 158),
    COLOR_38(164, 168, 184),
    COLOR_39(86, 88, 97),
    COLOR_40(106, 76, 54),
    COLOR_41(130, 94, 66),
    COLOR_42(151, 109, 77),
    COLOR_43(79, 57, 40),
    COLOR_44(79, 79, 79),
    COLOR_45(96, 96, 96),
    COLOR_46(112, 112, 112),
    COLOR_47(59, 59, 59),
    COLOR_48(45, 45, 180),
    COLOR_49(55, 55, 220),
    COLOR_50(64, 64, 255),
    COLOR_51(33, 33, 135),
    COLOR_52(100, 84, 50),
    COLOR_53(123, 102, 62),
    COLOR_54(143, 119, 72),
    COLOR_55(75, 63, 38),
    COLOR_56(180, 177, 172),
    COLOR_57(220, 217, 211),
    COLOR_58(255, 252, 245),
    COLOR_59(135, 133, 129),
    COLOR_60(152, 89, 36),
    COLOR_61(186, 109, 44),
    COLOR_62(216, 127, 51),
    COLOR_63(114, 67, 27),
    COLOR_64(125, 53, 152),
    COLOR_65(153, 65, 186),
    COLOR_66(178, 76, 216),
    COLOR_67(94, 40, 114),
    COLOR_68(72, 108, 152),
    COLOR_69(88, 132, 186),
    COLOR_70(102, 153, 216),
    COLOR_71(54, 81, 114),
    COLOR_72(161, 161, 36),
    COLOR_73(197, 197, 44),
    COLOR_74(229, 229, 51),
    COLOR_75(121, 121, 27),
    COLOR_76(89, 144, 17),
    COLOR_77(109, 176, 21),
    COLOR_78(127, 204, 25),
    COLOR_79(67, 108, 13),
    COLOR_80(170, 89, 116),
    COLOR_81(208, 109, 142),
    COLOR_82(242, 127, 165),
    COLOR_83(128, 67, 87),
    COLOR_84(53, 53, 53),
    COLOR_85(65, 65, 65),
    COLOR_86(76, 76, 76),
    COLOR_87(40, 40, 40),
    COLOR_88(108, 108, 108),
    COLOR_89(132, 132, 132),
    COLOR_90(153, 153, 153),
    COLOR_91(81, 81, 81),
    COLOR_92(53, 89, 108),
    COLOR_93(65, 109, 132),
    COLOR_94(76, 127, 153),
    COLOR_95(40, 67, 81),
    COLOR_96(89, 44, 125),
    COLOR_97(109, 54, 153),
    COLOR_98(127, 63, 178),
    COLOR_99(67, 33, 94),
    COLOR_100(36, 53, 125),
    COLOR_101(44, 65, 153),
    COLOR_102(51, 76, 178),
    COLOR_103(27, 40, 94),
    COLOR_104(72, 53, 36),
    COLOR_105(88, 65, 44),
    COLOR_106(102, 76, 51),
    COLOR_107(54, 40, 27),
    COLOR_108(72, 89, 36),
    COLOR_109(88, 109, 44),
    COLOR_110(102, 127, 51),
    COLOR_111(54, 67, 27),
    COLOR_112(108, 36, 36),
    COLOR_113(132, 44, 44),
    COLOR_114(153, 51, 51),
    COLOR_115(81, 27, 27),
    COLOR_116(17, 17, 17),
    COLOR_117(21, 21, 21),
    COLOR_118(25, 25, 25),
    COLOR_119(13, 13, 13),
    COLOR_120(176, 168, 54),
    COLOR_121(215, 205, 66),
    COLOR_122(250, 238, 77),
    COLOR_123(132, 126, 40),
    COLOR_124(64, 154, 150),
    COLOR_125(79, 188, 183),
    COLOR_126(92, 219, 213),
    COLOR_127(48, 115, 112),
    COLOR_128(52, 90, 180),
    COLOR_129(63, 110, 220),
    COLOR_130(74, 128, 255),
    COLOR_131(39, 67, 135),
    COLOR_132(0, 153, 40),
    COLOR_133(0, 187, 50),
    COLOR_134(0, 217, 58),
    COLOR_135(0, 114, 30),
    COLOR_136(91, 60, 34),
    COLOR_137(111, 74, 42),
    COLOR_138(129, 86, 49),
    COLOR_139(68, 45, 25),
    COLOR_140(79, 1, 0),
    COLOR_141(96, 1, 0),
    COLOR_142(112, 2, 0),
    COLOR_143(59, 1, 0),
    COLOR_144(147, 124, 113),
    COLOR_145(180, 152, 138),
    COLOR_146(209, 177, 161),
    COLOR_147(110, 93, 85),
    COLOR_148(112, 57, 25),
    COLOR_149(137, 70, 31),
    COLOR_150(159, 82, 36),
    COLOR_151(84, 43, 19),
    COLOR_152(105, 61, 76),
    COLOR_153(128, 75, 93),
    COLOR_154(149, 87, 108),
    COLOR_155(78, 46, 57),
    COLOR_156(79, 76, 97),
    COLOR_157(96, 93, 119),
    COLOR_158(112, 108, 138),
    COLOR_159(59, 57, 73),
    COLOR_160(131, 93, 25),
    COLOR_161(160, 114, 31),
    COLOR_162(186, 133, 36),
    COLOR_163(98, 70, 19),
    COLOR_164(72, 82, 37),
    COLOR_165(88, 100, 45),
    COLOR_166(103, 117, 53),
    COLOR_167(54, 61, 28),
    COLOR_168(112, 54, 55),
    COLOR_169(138, 66, 67),
    COLOR_170(160, 77, 78),
    COLOR_171(84, 40, 41),
    COLOR_172(40, 28, 24),
    COLOR_173(49, 35, 30),
    COLOR_174(57, 41, 35),
    COLOR_175(30, 21, 18),
    COLOR_176(95, 75, 69),
    COLOR_177(116, 92, 84),
    COLOR_178(135, 107, 98),
    COLOR_179(71, 56, 51),
    COLOR_180(61, 64, 64),
    COLOR_181(75, 79, 79),
    COLOR_182(87, 92, 92),
    COLOR_183(46, 48, 48),
    COLOR_184(86, 51, 62),
    COLOR_185(105, 62, 75),
    COLOR_186(122, 73, 88),
    COLOR_187(64, 38, 46),
    COLOR_188(53, 43, 64),
    COLOR_189(65, 53, 79),
    COLOR_190(76, 62, 92),
    COLOR_191(40, 32, 48),
    COLOR_192(53, 35, 24),
    COLOR_193(65, 43, 30),
    COLOR_194(76, 50, 35),
    COLOR_195(40, 26, 18),
    COLOR_196(53, 57, 29),
    COLOR_197(65, 70, 36),
    COLOR_198(76, 82, 42),
    COLOR_199(40, 43, 22),
    COLOR_200(100, 42, 32),
    COLOR_201(122, 51, 39),
    COLOR_202(142, 60, 46),
    COLOR_203(75, 31, 24),
    COLOR_204(26, 15, 11),
    COLOR_205(31, 18, 13),
    COLOR_206(37, 22, 16),
    COLOR_207(19, 11, 8);

    private static final MapColor[] VALUES = values();

    private final int red;
    private final int green;
    private final int blue;

    MapColor(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public static MapColor fromId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : COLOR_0;
    }

    public int toABGR() {
        int alpha = 255;
        if (red == -1 && green == -1 && blue == -1)
            alpha = 0; // transparent

        return ((alpha & 0xFF) << 24) |
               ((blue & 0xFF) << 16) |
               ((green & 0xFF) << 8) |
               ((red & 0xFF) << 0);
    }
}