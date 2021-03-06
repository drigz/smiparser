/**
 * Copyright 2011-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jsmiparser.smi;

import org.jsmiparser.phase.xref.XRefProblemReporter;
import org.jsmiparser.util.location.Location;
import org.jsmiparser.util.pair.Pair;
import org.jsmiparser.util.token.IdToken;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class SmiImports {

    // straight from libsmi, extended with macro definitions
    static final String[] V1_V2_MAP = {
            "RFC1155-SMI", "internet", "SNMPv2-SMI", "internet",
            "RFC1155-SMI", "directory", "SNMPv2-SMI", "directory",
            "RFC1155-SMI", "mgmt", "SNMPv2-SMI", "mgmt",
            "RFC1155-SMI", "experimental", "SNMPv2-SMI", "experimental",
            "RFC1155-SMI", "private", "SNMPv2-SMI", "private",
            "RFC1155-SMI", "enterprises", "SNMPv2-SMI", "enterprises",
            "RFC1155-SMI", "IpAddress", "SNMPv2-SMI", "IpAddress",
            "RFC1155-SMI", "Counter", "SNMPv2-SMI", "Counter32",
            "RFC1155-SMI", "Gauge", "SNMPv2-SMI", "Gauge32",
            "RFC1155-SMI", "TimeTicks", "SNMPv2-SMI", "TimeTicks",
            "RFC1155-SMI", "Opaque", "SNMPv2-SMI", "Opaque",
            "RFC1065-SMI", "internet", "SNMPv2-SMI", "internet",
            "RFC1065-SMI", "directory", "SNMPv2-SMI", "directory",
            "RFC1065-SMI", "mgmt", "SNMPv2-SMI", "mgmt",
            "RFC1065-SMI", "experimental", "SNMPv2-SMI", "experimental",
            "RFC1065-SMI", "private", "SNMPv2-SMI", "private",
            "RFC1065-SMI", "enterprises", "SNMPv2-SMI", "enterprises",
            "RFC1065-SMI", "IpAddress", "SNMPv2-SMI", "IpAddress",
            "RFC1065-SMI", "Counter", "SNMPv2-SMI", "Counter32",
            "RFC1065-SMI", "Gauge", "SNMPv2-SMI", "Gauge32",
            "RFC1065-SMI", "TimeTicks", "SNMPv2-SMI", "TimeTicks",
            "RFC1065-SMI", "Opaque", "SNMPv2-SMI", "Opaque",
            "RFC1213-MIB", "mib-2", "SNMPv2-SMI", "mib-2",
            "RFC1213-MIB", "DisplayString", "SNMPv2-TC", "DisplayString",

            "RFC-1212", "OBJECT-TYPE", "SNMPv2-SMI", "OBJECT-TYPE"
            // What to do with TRAP-TYPE?
    };


    private final SmiModule importerModule;
    private final IdToken moduleToken;
    private final List<IdToken> symbolTokens;

    private SmiModule module;
    private LinkedHashMap<String, SmiSymbol> symbolMap = new LinkedHashMap<String, SmiSymbol>();

    public SmiImports(SmiModule importerModule, IdToken moduleToken, List<IdToken> symbolTokens) {
        assert (importerModule != null);
        assert (moduleToken != null);
        assert (symbolTokens != null);

        this.importerModule = importerModule;

        this.moduleToken = moduleToken;
        this.symbolTokens = Collections.unmodifiableList(symbolTokens);
    }

    public SmiModule getModule() {
        return module;
    }

    public Collection<SmiSymbol> getSymbols() {
        return symbolMap.values();
    }

    public IdToken getModuleToken() {
        return moduleToken;
    }

    public List<IdToken> getSymbolTokens() {
        return symbolTokens;
    }

    public Location getLocation() {
        return moduleToken.getLocation();
    }

    public SmiSymbol find(String id) {
        return symbolMap.get(id);
    }

    public void resolveImports(XRefProblemReporter reporter) {
        module = importerModule.getMib().findModule(moduleToken.getId());
        if (module != null) {
            for (IdToken idToken : getSymbolTokens()) {
                SmiSymbol symbol = getModule().findSymbol(idToken.getId());
                if (symbol != null) {
                    symbolMap.put(idToken.getId(), symbol);
                } else {
                    reporter.reportCannotFindImportedSymbol(idToken, moduleToken);
                }
            }
        } else {
            if (importerModule.getMib().getOptions().isConvertV1ImportsToV2()) {
                resolveV1Imports(reporter);
            } else {
                reporter.reportCannotFindModule(moduleToken);
            }
        }
    }

    private void resolveV1Imports(XRefProblemReporter reporter) {
        for (IdToken idToken : getSymbolTokens()) {
            Pair<String, String> v2Definition = findV2Definition(idToken.getId());
            if (v2Definition != null) {
                // notice we are not setting the m_module in this case!
                SmiModule module = importerModule.getMib().findModule(v2Definition.getFirst());
                if (module != null) {
                    SmiSymbol symbol = module.findSymbol(v2Definition.getSecond());
                    if (symbol != null) {
                        symbolMap.put(idToken.getId(), symbol);
                    } else {
                        reporter.reportCannotFindImportedSymbol(idToken, moduleToken);
                    }
                } else {
                    reporter.reportCannotFindModule(moduleToken);
                }
            } else {
                reporter.reportCannotFindImportedSymbol(idToken, moduleToken);
            }
        }
    }

    public Pair<String, String> findV2Definition(String id) {
        for (int i = 0; i < V1_V2_MAP.length; i += 4) {
            String oldMib = V1_V2_MAP[i];
            String oldId = V1_V2_MAP[i + 1];
            if (oldMib.equals(moduleToken.getId())
                    && oldId.equals(id)) {
                return new Pair<String, String>(V1_V2_MAP[i + 2], V1_V2_MAP[i + 3]);
            }
        }
        return null;
    }

}
