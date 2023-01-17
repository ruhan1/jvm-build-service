package com.redhat.hacbs.container.verifier.asm;

import static com.redhat.hacbs.container.verifier.asm.AsmUtils.accessToSet;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ModuleExportNode;

public record ModuleExportInfo(String packaze, Set<ModuleAccess> access,
        List<String> modules) implements AsmDiffable<ModuleExportInfo> {
    public ModuleExportInfo(ModuleExportNode node) {
        this(node.packaze, accessToSet(node.access, ModuleAccess.class),
                node.modules != null ? List.copyOf(node.modules) : null);
    }
}