/*
 Copyright 2008-2010 Gephi
 Authors : Mathieu Bastian <mathieu.bastian@gephi.org>
 Website : http://www.gephi.org

 This file is part of Gephi.

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright 2011 Gephi Consortium. All rights reserved.

 The contents of this file are subject to the terms of either the GNU
 General Public License Version 3 only ("GPL") or the Common
 Development and Distribution License("CDDL") (collectively, the
 "License"). You may not use this file except in compliance with the
 License. You can obtain a copy of the License at
 http://gephi.org/about/legal/license-notice/
 or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
 specific language governing permissions and limitations under the
 License.  When distributing the software, include this License Header
 Notice in each file and include the License files at
 /cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
 License Header, with the fields enclosed by brackets [] replaced by
 your own identifying information:
 "Portions Copyrighted [year] [name of copyright owner]"

 If you wish your version of this file to be governed by only the CDDL
 or only the GPL Version 3, indicate your decision by adding
 "[Contributor] elects to include this software in this distribution
 under the [CDDL or GPL Version 3] license." If you do not indicate a
 single choice of license, a recipient has the option to distribute
 your version of this file under either the CDDL, the GPL Version 3 or
 to extend the choice of license to its licensees as provided above.
 However, if you add GPL Version 3 code and therefore, elected the GPL
 Version 3 license, then the option applies only if the new code is
 made subject to such option by the copyright holder.

 Contributor(s):

 Portions Copyrighted 2011 Gephi Consortium.
 */
package org.gephi.filters.plugin.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JPanel;
import org.gephi.attribute.api.AttributeModel;
import org.gephi.attribute.api.AttributeUtils;
import org.gephi.attribute.api.Column;
import org.gephi.attribute.time.TimestampBooleanSet;
import org.gephi.attribute.time.TimestampStringSet;
import org.gephi.filters.api.FilterLibrary;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.AbstractAttributeFilter;
import org.gephi.filters.plugin.AbstractAttributeFilterBuilder;
import org.gephi.filters.plugin.DynamicAttributesHelper;
import org.gephi.filters.spi.*;
import org.gephi.graph.api.*;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Mathieu Bastian
 */
@ServiceProvider(service = CategoryBuilder.class)
public class AttributeEqualBuilder implements CategoryBuilder {

    private final static Category EQUAL = new Category(
            NbBundle.getMessage(AttributeEqualBuilder.class, "AttributeEqualBuilder.name"),
            null,
            FilterLibrary.ATTRIBUTES);

    public Category getCategory() {
        return EQUAL;
    }

    public FilterBuilder[] getBuilders() {
        List<FilterBuilder> builders = new ArrayList<FilterBuilder>();
        AttributeModel am = Lookup.getDefault().lookup(AttributeModel.class);
        List<Column> columns = new ArrayList<Column>();
        columns.addAll(Arrays.asList(am.getNodeTable().getColumns()));
        columns.addAll(Arrays.asList(am.getEdgeTable().getColumns()));
        for (Column c : columns) {
//            if (AttributeUtils.isStringColumn(c) || c.getTypeClass().equals(AttributeType.DYNAMIC_STRING)) {
            if (c.getTypeClass().equals(String.class) || c.getTypeClass().equals(TimestampStringSet.class)) {
                EqualStringFilterBuilder b = new EqualStringFilterBuilder(c);
                builders.add(b);
//            } else if (AttributeUtils.getDefault().isNumberColumn(c) || AttributeUtils.getDefault().isDynamicNumberColumn(c)) {
            } else if (AttributeUtils.isNumberType(c.getTypeClass())) {
                EqualNumberFilterBuilder b = new EqualNumberFilterBuilder(c);
                builders.add(b);
//            } else if (c.getType().equals(AttributeType.BOOLEAN) || c.getType().equals(AttributeType.DYNAMIC_BOOLEAN)) {
            } else if (c.getTypeClass().equals(Boolean.class) || c.getTypeClass().equals(TimestampBooleanSet.class)) {
                EqualBooleanFilterBuilder b = new EqualBooleanFilterBuilder(c);
                builders.add(b);
            }
        }
        return builders.toArray(new FilterBuilder[0]);
    }

    private static class EqualStringFilterBuilder extends AbstractAttributeFilterBuilder {

        public EqualStringFilterBuilder(Column column) {
            super(column,
                    EQUAL,
                    NbBundle.getMessage(AttributeEqualBuilder.class, "AttributeEqualBuilder.description"),
                    null);
        }

        public EqualStringFilter getFilter() {
            return new EqualStringFilter(column);
        }

        public JPanel getPanel(Filter filter) {
            EqualStringUI ui = Lookup.getDefault().lookup(EqualStringUI.class);
            if (ui != null) {
                return ui.getPanel((EqualStringFilter) filter);
            }
            return null;
        }
    }

    public static class EqualStringFilter extends AbstractAttributeFilter {

        private String pattern;
        private boolean useRegex;
        private Pattern regex;
//        private DynamicAttributesHelper dynamicHelper = new DynamicAttributesHelper(this, null);

        public EqualStringFilter(Column column) {
            super(NbBundle.getMessage(AttributeEqualBuilder.class, "AttributeEqualBuilder.name"),
                    column);

            //Add ptoperties
            addProperty(String.class, "pattern");
            addProperty(Boolean.class, "useRegex");
        }

        public boolean init(Graph graph) {
//            dynamicHelper = new DynamicAttributesHelper(this, graph);
            return true;
        }

        public boolean evaluate(Graph graph, Element element) {
            if (pattern == null) {
                return true;
            }
            Object val = element.getAttribute(column);
//            val = dynamicHelper.getDynamicValue(val);
            if (val != null && useRegex) {
                return regex.matcher(val.toString()).matches();
            } else if (val != null) {
                return pattern.equals(val.toString());
            }
            return false;
        }

        public void finish() {
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
            this.regex = Pattern.compile(pattern);
        }

        public boolean isUseRegex() {
            return useRegex;
        }

        public void setUseRegex(boolean useRegex) {
            this.useRegex = useRegex;
        }
    }

    private static class EqualNumberFilterBuilder extends AbstractAttributeFilterBuilder {

        public EqualNumberFilterBuilder(Column column) {
            super(column,
                    EQUAL,
                    NbBundle.getMessage(AttributeEqualBuilder.class, "AttributeEqualBuilder.description"),
                    null);
        }

        public EqualNumberFilter getFilter() {
            return new EqualNumberFilter(column);

        }

        public JPanel getPanel(Filter filter) {
            EqualNumberUI ui = Lookup.getDefault().lookup(EqualNumberUI.class);
            if (ui != null) {
                return ui.getPanel((EqualNumberFilter) filter);
            }
            return null;
        }
    }

    public static class EqualNumberFilter extends AbstractAttributeFilter implements RangeFilter {

        private Number match;
        private Range range;
//        private DynamicAttributesHelper dynamicHelper = new DynamicAttributesHelper(this, null);

        public EqualNumberFilter(Column column) {
            super(NbBundle.getMessage(AttributeEqualBuilder.class, "AttributeEqualBuilder.name"), column);

            //App property
            addProperty(Number.class, "match");
            addProperty(Range.class, "range");
        }

        public boolean init(Graph graph) {
//            if (AttributeUtils.getDefault().isNodeColumn(column)) {
            if (AttributeUtils.isNodeColumn(column)) {
                if (graph.getNodeCount() == 0) {
                    return false;
                }
            } else if (AttributeUtils.isEdgeColumn(column)) {
                if (graph.getEdgeCount() == 0) {
                    return false;
                }
            }
//            dynamicHelper = new DynamicAttributesHelper(this, graph);
            return true;
        }

        @Override
        public boolean evaluate(Graph graph, Element element) {
            Object val = element.getAttribute(column);
//            val = dynamicHelper.getDynamicValue(val);
            if (val != null) {
                return val.equals(match);
            }
            return false;
        }

        public void finish() {
        }

        public Number[] getValues(Graph graph) {
            List<Number> vals = new ArrayList<Number>();
//            if (AttributeUtils.getDefault().isNodeColumn(column)) {
            if (AttributeUtils.isNodeColumn(column)) {
                for (Node n : graph.getNodes()) {
                    Object val = n.getAttribute(column);
//                    val = dynamicHelper.getDynamicValue(val);
                    if (val != null) {
                        vals.add((Number) val);
                    }
                }
            } else {
                for (Edge e : graph.getEdges()) {
                    Object val = e.getAttribute(column);
//                    Object val = e.getEdgeData().getAttributes().getValue(column.getIndex());
//                    val = dynamicHelper.getDynamicValue(val);
                    if (val != null) {
                        vals.add((Number) val);
                    }
                }
            }
            return vals.toArray(new Number[0]);
        }

        public FilterProperty getRangeProperty() {
            return getProperties()[2];
        }

        public Range getRange() {
            return range;
        }

        public void setRange(Range range) {
            this.range = range;
            if(match == null) {
                match = range.getMinimum();
            } else {
                match = Range.trimToBounds(range.getMinimum(), range.getMaximum(), match);
            }
        }

        public Number getMatch() {
            return match;
        }

        public void setMatch(Number match) {
            this.match = match;
        }
    }

    private static class EqualBooleanFilterBuilder extends AbstractAttributeFilterBuilder {

        public EqualBooleanFilterBuilder(Column column) {
            super(column,
                    EQUAL,
                    NbBundle.getMessage(AttributeEqualBuilder.class, "AttributeEqualBuilder.description"), null);
        }

        public EqualBooleanFilter getFilter() {
            return new EqualBooleanFilter(column);
        }

        public JPanel getPanel(Filter filter) {
            EqualBooleanUI ui = Lookup.getDefault().lookup(EqualBooleanUI.class);
            if (ui != null) {
                return ui.getPanel((EqualBooleanFilter) filter);
            }
            return null;
        }
    }

    public static class EqualBooleanFilter extends AbstractAttributeFilter {

        private boolean match = false;
//        private DynamicAttributesHelper dynamicHelper = new DynamicAttributesHelper(this, null);

        public EqualBooleanFilter(Column column) {
            super(NbBundle.getMessage(AttributeEqualBuilder.class, "AttributeEqualBuilder.name"),
                    column);

            //Add property
            addProperty(Boolean.class, "match");
        }

        public boolean init(Graph graph) {
//            dynamicHelper = new DynamicAttributesHelper(this, graph);
            return true;
        }

        public boolean evaluate(Graph graph, Element attributable) {
            Object val = attributable.getAttribute(column);
            if (val != null) {
                return val.equals(match);
            }
            return false;
        }

        public void finish() {
        }

        public boolean isMatch() {
            return match;
        }

        public void setMatch(boolean match) {
            this.match = match;
        }
    }
}
