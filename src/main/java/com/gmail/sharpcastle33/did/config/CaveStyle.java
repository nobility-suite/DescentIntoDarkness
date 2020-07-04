package com.gmail.sharpcastle33.did.config;

import com.gmail.sharpcastle33.did.generator.PainterStep;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CaveStyle {
    private List<PainterStep> painterSteps = new ArrayList<>();

    public void serialize(ConfigurationSection map) {
        map.set("painterSteps", painterSteps.stream().map(PainterStep::serialize).collect(Collectors.toCollection(ArrayList::new)));
    }

    public static CaveStyle deserialize(ConfigurationSection map) {
        CaveStyle style = new CaveStyle();
        List<?> painterSteps = map.getList("painterSteps");
        if (painterSteps != null) {
            painterSteps.stream().map(PainterStep::deserialize).forEachOrdered(style.painterSteps::add);
        }
        return style;
    }

    public List<PainterStep> getPainterSteps() {
        return painterSteps;
    }

    public void setPainterSteps(List<PainterStep> painterSteps) {
        this.painterSteps = painterSteps;
    }
}
