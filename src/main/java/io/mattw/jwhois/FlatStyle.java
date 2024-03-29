package io.mattw.jwhois;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubIJTheme;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import java.util.concurrent.Callable;

public enum FlatStyle {
    FLATLAF_MACOS_DARK("FlatLaf MacOS Dark", FlatMacDarkLaf::setup),
    FLATLAF_MACOS_LIGHT("FlatLaf MacOS Light", FlatMacLightLaf::setup),
    GITHUB_DARK("GitHub Dark", FlatGitHubDarkIJTheme::setup),
    GITHUB_LIGHT("GitHub Light", FlatGitHubIJTheme::setup),
    INTELLIJ_DARCULAR("IntelliJ Darcula", FlatDarculaLaf::setup),
    INTELLIJ_LIGHT("IntelliJ Light", FlatLightLaf::setup),
    ;

    private String displayName;
    private String prefName;
    private Callable<Boolean> lafSetup;

    FlatStyle(String displayName, Callable<Boolean> lafSetup) {
        this.displayName = displayName;
        this.prefName = name().toLowerCase();
        this.lafSetup = lafSetup;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrefName() {
        return prefName;
    }

    public Callable<Boolean> getLafSetup() {
        return lafSetup;
    }

    public static FlatStyle fromPrefName(String prefName) {
        for (FlatStyle style : values()) {
            if (style.prefName.equalsIgnoreCase(prefName)) {
                return style;
            }
        }

        return INTELLIJ_DARCULAR;
    }

    public static void setupFromPrefName(String prefName) {
        setup(fromPrefName(prefName));
    }

    public static void setup(FlatStyle style) {
        try {
            style.getLafSetup().call();

            FlatLaf.updateUI();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
