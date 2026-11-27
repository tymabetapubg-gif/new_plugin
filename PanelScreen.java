package ua.tymaa.adminpanelmod;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Екран адмін панелі у стилі "термінал" (темно-зелений). */
public class PanelScreen extends Screen {

    enum Arg { NONE, NICK, TEXT, NUMBER }

    record Act(String cat, String title, String desc, Item icon, String cmd, Arg arg, boolean danger) { }

    // ------------------------------------------------------------ кольори
    private static final int C_DIM = 0xB0000000;
    private static final int C_BG = 0xF2070F0B;
    private static final int C_BORDER = 0xFF1F9D5A;
    private static final int C_GREEN = 0xFF3DFF8A;
    private static final int C_TEXT = 0xFFF2F2F2;
    private static final int C_GRAY = 0xFF8A9A90;
    private static final int C_CARD = 0xFF0B1A12;
    private static final int C_CARD_B = 0xFF1B4D33;
    private static final int C_CARD_H = 0xFF2FD07A;
    private static final int C_SEL = 0xFF10301F;
    private static final int C_WARN = 0xFFFFC107;
    private static final int C_RED = 0xFFFF5555;
    private static final int C_TERM = 0xFF040B07;

    private static final String[] CATS = {"ВСІ", "Я", "ГРАВЦІ", "СВІТ", "ЧАС", "СЕРВЕР"};

    private static final List<Act> ACTS = new ArrayList<>();

    private static void a(String cat, String title, String desc, Item icon, String cmd, Arg arg, boolean danger) {
        ACTS.add(new Act(cat, title, desc, icon, cmd, arg, danger));
    }

    static {
        // --- Я
        a("Я", "КРЕАТИВ", "Режим гри: креатив", Items.DIAMOND_BLOCK, "gm_creative", Arg.NONE, false);
        a("Я", "ВИЖИВАННЯ", "Режим гри: виживання", Items.IRON_SWORD, "gm_survival", Arg.NONE, false);
        a("Я", "ПРИГОДА", "Режим гри: пригода", Items.COMPASS, "gm_adventure", Arg.NONE, false);
        a("Я", "СПОСТЕРІГАЧ", "Режим гри: спостерігач", Items.ENDER_EYE, "gm_spectator", Arg.NONE, false);
        a("Я", "ПОЛІТ", "Увімкнути або вимкнути політ", Items.ELYTRA, "fly", Arg.NONE, false);
        a("Я", "БЕЗСМЕРТЯ", "Ти не отримуєш урон", Items.TOTEM_OF_UNDYING, "god", Arg.NONE, false);
        a("Я", "НЕВИДИМКА", "Тебе не бачать звичайні гравці", Items.GLASS, "vanish", Arg.NONE, false);
        a("Я", "НІЧНЕ БАЧЕННЯ", "Постійне нічне бачення", Items.GOLDEN_CARROT, "nv", Arg.NONE, false);
        a("Я", "ШВИДКІСТЬ", "Швидкість 1-10 (поле аргументу), стандарт 2", Items.SUGAR, "speed", Arg.NUMBER, false);
        a("Я", "ВИЛІКУВАТИ", "Повне здоров'я, їжа, погасити вогонь", Items.GOLDEN_APPLE, "heal", Arg.NONE, false);
        a("Я", "НАГОДУВАТИ", "Повна ситість", Items.COOKED_BEEF, "feed", Arg.NONE, false);
        a("Я", "ПОГАСИТИ ВОГОНЬ", "Зняти горіння з себе", Items.WATER_BUCKET, "extinguish", Arg.NONE, false);
        a("Я", "ПОЛАГОДИТИ", "Відремонтувати предмет у руці", Items.ANVIL, "repair", Arg.NONE, false);
        a("Я", "НА ПОВЕРХНЮ", "Телепорт на найвищий блок над собою", Items.LADDER, "top", Arg.NONE, false);
        a("Я", "ОЧИСТИТИ ІНВЕНТАР", "Видаляє всі твої предмети (двічі натисни)", Items.BARRIER, "clear_inv", Arg.NONE, true);

        // --- ГРАВЦІ (нік у полі аргументу)
        a("ГРАВЦІ", "ТП ДО НЬОГО", "Телепорт до гравця (нік)", Items.ENDER_PEARL, "tp", Arg.NICK, false);
        a("ГРАВЦІ", "ТП ЙОГО ДО МЕНЕ", "Гравця телепортує до тебе", Items.ENDER_EYE, "tphere", Arg.NICK, false);
        a("ГРАВЦІ", "ВИЛІКУВАТИ ЙОГО", "Повне здоров'я гравцю", Items.SPLASH_POTION, "heal_p", Arg.NICK, false);
        a("ГРАВЦІ", "НАГОДУВАТИ ЙОГО", "Повна ситість гравцю", Items.BREAD, "feed_p", Arg.NICK, false);
        a("ГРАВЦІ", "ЗАМОРОЗИТИ", "Заморозити або розморозити гравця", Items.ICE, "freeze_p", Arg.NICK, false);
        a("ГРАВЦІ", "ІНВЕНТАР ГРАВЦЯ", "Відкрити його інвентар", Items.CHEST, "invsee", Arg.NICK, false);
        a("ГРАВЦІ", "ЕНДЕР-СКРИНЯ", "Відкрити його ендер-скриню", Items.ENDER_CHEST, "echest", Arg.NICK, false);
        a("ГРАВЦІ", "КІКНУТИ", "Вигнати гравця з сервера (двічі натисни)", Items.IRON_DOOR, "kick", Arg.NICK, true);
        a("ГРАВЦІ", "ХТО ОНЛАЙН", "Список гравців на сервері", Items.PLAYER_HEAD, "online", Arg.NONE, false);

        // --- СВІТ
        a("СВІТ", "ВБИТИ ВОРОГІВ", "Ворожі моби навколо, радіус у полі аргументу", Items.ZOMBIE_HEAD, "kill_hostile", Arg.NUMBER, false);
        a("СВІТ", "ПРИБРАТИ ПРЕДМЕТИ", "Видаляє всі предмети на землі", Items.HOPPER, "clear_drops", Arg.NONE, false);
        a("СВІТ", "МЕЖА СВІТУ", "Розмір межі світу, число в полі аргументу", Items.STRUCTURE_VOID, "border", Arg.NUMBER, false);
        a("СВІТ", "СПАВН ТУТ", "Точка спавну світу = де ти стоїш", Items.RED_BED, "setspawn", Arg.NONE, false);
        a("СВІТ", "НА СПАВН", "Телепорт на спавн світу", Items.COMPASS, "spawn", Arg.NONE, false);
        a("СВІТ", "БЛИСКАВКА", "Ефект блискавки на твоєму місці", Items.LIGHTNING_ROD, "lightning", Arg.NONE, false);

        // --- ЧАС
        a("ЧАС", "ДЕНЬ", "Встановити ранок", Items.CLOCK, "time_day", Arg.NONE, false);
        a("ЧАС", "ПОЛУДЕНЬ", "Встановити полудень", Items.SUNFLOWER, "time_noon", Arg.NONE, false);
        a("ЧАС", "НІЧ", "Встановити ніч", Items.COAL, "time_night", Arg.NONE, false);
        a("ЧАС", "ПІВНІЧ", "Встановити північ", Items.OBSIDIAN, "time_midnight", Arg.NONE, false);
        a("ЧАС", "ЯСНО", "Прибрати дощ і грозу", Items.YELLOW_DYE, "weather_clear", Arg.NONE, false);
        a("ЧАС", "ДОЩ", "Увімкнути дощ", Items.WATER_BUCKET, "weather_rain", Arg.NONE, false);
        a("ЧАС", "ГРОЗА", "Увімкнути грозу", Items.LIGHTNING_ROD, "weather_thunder", Arg.NONE, false);
        a("ЧАС", "СТОП ЧАС", "Зупинити або запустити зміну дня і ночі", Items.PACKED_ICE, "freeze_time", Arg.NONE, false);

        // --- СЕРВЕР
        a("СЕРВЕР", "ОГОЛОШЕННЯ", "Повідомлення всім (текст у полі аргументу)", Items.BELL, "broadcast", Arg.TEXT, false);
        a("СЕРВЕР", "МИРНО", "Складність: мирно", Items.EMERALD, "difficulty peaceful", Arg.NONE, false);
        a("СЕРВЕР", "ЛЕГКО", "Складність: легко", Items.IRON_INGOT, "difficulty easy", Arg.NONE, false);
        a("СЕРВЕР", "НОРМАЛЬНО", "Складність: нормально", Items.GOLD_INGOT, "difficulty normal", Arg.NONE, false);
        a("СЕРВЕР", "СКЛАДНО", "Складність: складно", Items.DIAMOND, "difficulty hard", Arg.NONE, false);
        a("СЕРВЕР", "ЗБЕРЕГТИ ІНВЕНТАР", "Інвентар лишається після смерті", Items.SHULKER_BOX, "keepinv", Arg.NONE, false);
        a("СЕРВЕР", "МОБИ ЛАМАЮТЬ", "Чи ламають моби блоки", Items.CREEPER_HEAD, "mobgrief", Arg.NONE, false);
        a("СЕРВЕР", "PVP", "Увімкнути або вимкнути PvP", Items.DIAMOND_SWORD, "pvp", Arg.NONE, false);
        a("СЕРВЕР", "ЗБЕРЕГТИ СВІТ", "Зберегти світ і гравців", Items.WRITABLE_BOOK, "save", Arg.NONE, false);
        a("СЕРВЕР", "TPS", "Показати TPS сервера", Items.REDSTONE, "tps", Arg.NONE, false);
    }

    // ------------------------------------------------------------ стан
    private int cat = 0;
    private double scroll = 0;
    private double maxScroll = 0;
    private String searchText = "";
    private String argText = "";
    private String status = "";
    private long statusTime = 0;
    private Act pending = null;
    private long pendingTime = 0;
    private boolean wasDown;

    private EditBox search;
    private EditBox argBox;

    // розмітка
    private int px, py, pw, ph, sw, cx, cw, listTop, listBottom;

    public PanelScreen() {
        super(Component.literal("Адмін панель"));
        this.wasDown = Minecraft.getInstance().mouseHandler.isLeftPressed();
    }

    @Override
    protected void init() {
        pw = Math.min(width - 16, 700);
        ph = Math.min(height - 16, 400);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        sw = pw < 420 ? 96 : 128;
        cx = px + sw + 10;
        cw = px + pw - 10 - cx;
        listTop = py + 76;
        listBottom = py + ph - 8;

        int searchW = Math.min(170, cw / 2);
        search = new EditBox(font, px + pw - 10 - searchW + 5, py + 28, searchW - 10, 12, Component.literal("Пошук"));
        search.setBordered(false);
        search.setMaxLength(32);
        search.setHint(Component.literal("Пошук"));
        search.setValue(searchText);
        search.setResponder(s -> { searchText = s; scroll = 0; });
        addRenderableWidget(search);

        int argX = cx + 74;
        argBox = new EditBox(font, argX + 5, py + 56, Math.min(200, cw - 80) - 10, 12, Component.literal("Аргумент"));
        argBox.setBordered(false);
        argBox.setMaxLength(200);
        argBox.setHint(Component.literal("нік / текст / число"));
        argBox.setValue(argText);
        argBox.setResponder(s -> argText = s);
        addRenderableWidget(argBox);
    }

    // ------------------------------------------------------------ допоміжне
    private List<Act> visible() {
        List<Act> out = new ArrayList<>();
        String q = searchText.trim().toLowerCase();
        for (Act act : ACTS) {
            if (cat != 0 && !act.cat().equals(CATS[cat])) continue;
            if (!q.isEmpty() && !(act.title().toLowerCase().contains(q) || act.desc().toLowerCase().contains(q))) continue;
            out.add(act);
        }
        return out;
    }

    private int count(int c) {
        if (c == 0) return ACTS.size();
        int n = 0;
        for (Act act : ACTS) if (act.cat().equals(CATS[c])) n++;
        return n;
    }

    private int cols() { return cw >= 380 ? 2 : 1; }
    private int gap() { return 8; }
    private int cardH() { return 44; }
    private int cardW() { return (cw - 8 - gap() * (cols() - 1)) / cols(); }

    private void setStatus(String s) {
        status = s;
        statusTime = System.currentTimeMillis();
    }

    private void box(GuiGraphics g, int x, int y, int w, int h, int border, int fill) {
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font, s, x, y, color, false);
    }

    private void bold(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font, Component.literal(s).withStyle(ChatFormatting.BOLD), x, y, color, false);
    }

    // ------------------------------------------------------------ малювання
    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        Font f = font;
        Minecraft mc = Minecraft.getInstance();

        g.fill(0, 0, width, height, C_DIM);
        box(g, px, py, pw, ph, C_BORDER, C_BG);

        // термінальний рядок
        g.fill(px + 1, py + 1, px + pw - 1, py + 17, C_TERM);
        String cursor = (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : " ";
        text(g, "tymaa@admin-panel:~$ ./панель" + cursor, px + 8, py + 5, C_GREEN);
        text(g, "x", px + pw - 14, py + 5, C_GRAY);

        // бокова панель
        g.fill(px + sw, py + 17, px + sw + 1, py + ph - 1, C_CARD_B);
        bold(g, "АДМІН ПАНЕЛЬ", px + 10, py + 26, C_GREEN);
        text(g, "доступ: ADMIN", px + 10, py + 38, C_GRAY);

        int rowY = py + 56;
        for (int i = 0; i < CATS.length; i++) {
            int ry = rowY + i * 24;
            boolean sel = i == cat;
            boolean hov = mx >= px + 6 && mx < px + sw - 6 && my >= ry && my < ry + 20;
            if (sel) box(g, px + 6, ry, sw - 12, 20, C_BORDER, C_SEL);
            else if (hov) box(g, px + 6, ry, sw - 12, 20, C_CARD_B, C_CARD);
            text(g, CATS[i], px + 14, ry + 6, sel ? C_GREEN : C_TEXT);
            String n = String.valueOf(count(i));
            text(g, n, px + sw - 14 - f.width(n), ry + 6, C_GRAY);
        }

        // підвал бокової панелі: нік
        String name = mc.player != null ? mc.player.getName().getString() : "player";
        g.fill(px + 6, py + ph - 34, px + sw - 6, py + ph - 33, C_CARD_B);
        bold(g, f.plainSubstrByWidth(name, sw - 20), px + 10, py + ph - 28, C_TEXT);
        text(g, "tymaa", px + 10, py + ph - 17, C_GRAY);

        // шапка
        String head = cat == 0 ? "ВСІ КНОПКИ" : CATS[cat] + " КНОПКИ";
        text(g, "~/", cx, py + 28, C_GREEN);
        bold(g, head, cx + 14, py + 28, C_TEXT);
        List<Act> list = visible();
        text(g, "[" + list.size() + "]", cx + 14 + f.width(head) + 8, py + 28, C_GRAY);

        // пошук та аргумент: фони
        int searchW = Math.min(170, cw / 2);
        box(g, px + pw - 10 - searchW, py + 24, searchW, 20, C_CARD_B, C_CARD);
        text(g, "аргумент:", cx, py + 57, C_GRAY);
        int argW = Math.min(200, cw - 80);
        box(g, cx + 74, py + 52, argW, 20, C_CARD_B, C_CARD);

        long now = System.currentTimeMillis();
        if (!status.isEmpty() && now - statusTime < 5000) {
            int sx = cx + 74 + argW + 8;
            int room = px + pw - 10 - sx;
            if (room > 40) {
                boolean err = status.startsWith("!");
                String s = err ? status.substring(1) : status;
                text(g, f.plainSubstrByWidth(s, room), sx, py + 58, err ? C_RED : (status.startsWith("?") ? C_WARN : C_GREEN));
            }
        }

        // картки
        int viewH = listBottom - listTop;
        int rows = (list.size() + cols() - 1) / cols();
        int contentH = rows * (cardH() + gap());
        maxScroll = Math.max(0, contentH - viewH);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        g.enableScissor(cx, listTop, cx + cw, listBottom);
        for (int i = 0; i < list.size(); i++) {
            Act act = list.get(i);
            int col = i % cols();
            int row = i / cols();
            int x = cx + col * (cardW() + gap());
            int y = listTop + row * (cardH() + gap()) - (int) scroll;
            if (y + cardH() < listTop || y > listBottom) continue;

            boolean hov = mx >= x && mx < x + cardW() && my >= Math.max(y, listTop) && my < Math.min(y + cardH(), listBottom);
            boolean pend = act == pending && now - pendingTime < 2500;
            box(g, x, y, cardW(), cardH(), pend ? C_WARN : (hov ? C_CARD_H : C_CARD_B), C_CARD);

            box(g, x + 7, y + 8, 28, 28, C_CARD_B, 0xFF0F241A);
            g.renderItem(new ItemStack(act.icon()), x + 13, y + 14);

            bold(g, f.plainSubstrByWidth(act.title(), cardW() - 48 - 62), x + 42, y + 7, C_TEXT);

            List<FormattedCharSequence> lines = f.split(Component.literal(act.desc()), cardW() - 50);
            for (int l = 0; l < Math.min(2, lines.size()); l++) {
                g.drawString(f, lines.get(l), x + 42, y + 20 + l * 10, C_GRAY, false);
            }

            String tag = null;
            int tagColor = C_GRAY;
            if (act.danger()) { tag = "ПІДТВЕРДИТИ"; tagColor = C_WARN; }
            else if (act.arg() == Arg.NICK) tag = "НІК";
            else if (act.arg() == Arg.TEXT) tag = "ТЕКСТ";
            else if (act.arg() == Arg.NUMBER) tag = "ЧИСЛО";
            if (tag != null) {
                int tw = f.width(tag) + 8;
                box(g, x + cardW() - tw - 6, y + 5, tw, 12, tagColor, 0xFF0A120D);
                text(g, tag, x + cardW() - tw - 2, y + 7, tagColor);
            }
        }
        g.disableScissor();

        if (list.isEmpty()) {
            text(g, "Нічого не знайдено", cx + 4, listTop + 6, C_GRAY);
        }

        // смуга прокрутки
        if (maxScroll > 0) {
            int barX = cx + cw - 4;
            int barH = Math.max(20, (int) (viewH * (viewH / (double) contentH)));
            int barY = listTop + (int) ((viewH - barH) * (scroll / maxScroll));
            g.fill(barX, listTop, barX + 3, listBottom, 0xFF0F241A);
            g.fill(barX, barY, barX + 3, barY + barH, C_GREEN);
        }

        super.render(g, mx, my, partial); // поля вводу

        boolean down = mc.mouseHandler.isLeftPressed();
        if (down && !wasDown) click(mx, my);
        wasDown = down;
    }

    // ------------------------------------------------------------ вхід
    private void click(int mx, int my) {
        // закрити
        if (mx >= px + pw - 20 && mx < px + pw && my >= py && my < py + 17) {
            Minecraft.getInstance().setScreen(null);
            return;
        }
        // категорії
        int rowY = py + 56;
        for (int i = 0; i < CATS.length; i++) {
            int ry = rowY + i * 24;
            if (mx >= px + 6 && mx < px + sw - 6 && my >= ry && my < ry + 20) {
                cat = i;
                scroll = 0;
                return;
            }
        }
        // картки
        if (my < listTop || my >= listBottom || mx < cx || mx >= cx + cw) return;
        List<Act> list = visible();
        for (int i = 0; i < list.size(); i++) {
            int col = i % cols();
            int row = i / cols();
            int x = cx + col * (cardW() + gap());
            int y = listTop + row * (cardH() + gap()) - (int) scroll;
            if (mx >= x && mx < x + cardW() && my >= y && my < y + cardH()) {
                run(list.get(i));
                return;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - scrollY * 26));
        return true;
    }

    private void run(Act act) {
        long now = System.currentTimeMillis();
        if (act.danger()) {
            if (pending != act || now - pendingTime > 2500) {
                pending = act;
                pendingTime = now;
                setStatus("?Натисни ще раз: " + act.title());
                return;
            }
            pending = null;
        }

        String arg = argText.trim();
        String cmd = "apanel " + act.cmd();
        switch (act.arg()) {
            case NICK -> {
                if (arg.isEmpty()) { setStatus("!Введи нік у поле аргументу"); return; }
                cmd += " " + arg.split("\\s+")[0];
            }
            case TEXT -> {
                if (arg.isEmpty()) { setStatus("!Введи текст у поле аргументу"); return; }
                cmd += " " + arg;
            }
            case NUMBER -> {
                if (!arg.isEmpty()) {
                    try { Double.parseDouble(arg.replace(',', '.')); }
                    catch (NumberFormatException e) { setStatus("!Аргумент має бути числом"); return; }
                    cmd += " " + arg.replace(',', '.');
                }
            }
            default -> { }
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.connection.sendCommand(cmd);
        setStatus("/" + cmd);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
