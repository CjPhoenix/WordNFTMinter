package me.phoenix.wnm.executables;

import me.phoenix.wnm.Main;

public class ProduceSingle {
    public static void main(String[] args) {
        if (args.length > 0) {
            if ("offline".equals(args[0])) {
                Main.main(new String[]{"1", "offline"});
            } else {
                Main.main(new String[]{"1"});
            }
        } else {
            Main.main(new String[]{"1"});
        }
    }
}
