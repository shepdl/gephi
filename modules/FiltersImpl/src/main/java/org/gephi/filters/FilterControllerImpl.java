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
package org.gephi.filters;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import java.beans.PropertyEditorManager;
import javax.swing.text.html.HTML;
import org.gephi.attribute.api.AttributeModel;
import org.gephi.attribute.api.Column;
import org.gephi.attribute.api.Origin;
//import org.gephi.data.attributes.api.AttributeOrigin;
//import org.gephi.data.attributes.api.AttributeType;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.FilterModel;
import org.gephi.filters.api.PropertyExecutor;
import org.gephi.filters.api.Query;
import org.gephi.filters.api.Range;
import org.gephi.filters.FilterThread.PropertyModifier;
import org.gephi.filters.spi.*;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.Node;
import org.gephi.project.api.ProjectController;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.gephi.utils.progress.ProgressTicketProvider;
import org.gephi.project.api.Workspace;
import org.gephi.project.api.WorkspaceInformation;
import org.gephi.project.api.WorkspaceListener;
import org.gephi.visualization.api.VisualizationController;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author Mathieu Bastian
 */
@ServiceProviders({
    @ServiceProvider(service = FilterController.class),
    @ServiceProvider(service = PropertyExecutor.class)})
public class FilterControllerImpl implements FilterController, PropertyExecutor {

    private FilterModelImpl model;

    public FilterControllerImpl() {
        //Register range editor
        PropertyEditorManager.registerEditor(Range.class, RangePropertyEditor.class);
        PropertyEditorManager.registerEditor(Column.class, AttributeColumnPropertyEditor.class);

        //Model management
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.addWorkspaceListener(new WorkspaceListener() {

            public void initialize(Workspace workspace) {
                workspace.add(new FilterModelImpl(workspace));
            }

            public void select(Workspace workspace) {
                model = (FilterModelImpl) workspace.getLookup().lookup(FilterModel.class);
                if (model == null) {
                    model = new FilterModelImpl(workspace);
                    workspace.add(model);
                }
            }

            public void unselect(Workspace workspace) {
            }

            public void close(Workspace workspace) {
                FilterModelImpl m = (FilterModelImpl) workspace.getLookup().lookup(FilterModel.class);
                if (m != null) {
                    m.destroy();
                }
            }

            public void disable() {
                GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
                if (model != null && model.getCurrentResult() != null && graphModel != null) {
                    graphModel.destroyView(model.getCurrentResult());
                    model.setCurrentResult(null);
                }
                model = null;
            }
        });
        if (pc.getCurrentWorkspace() != null) {
            Workspace workspace = pc.getCurrentWorkspace();
            model = (FilterModelImpl) workspace.getLookup().lookup(FilterModel.class);
            if (model == null) {
                model = new FilterModelImpl(workspace);
                workspace.add(model);
            }
        }
    }

    public Query createQuery(Filter filter) {
        if (filter instanceof Operator) {
            return new OperatorQueryImpl((Operator) filter);
        }
        return new FilterQueryImpl(filter);
    }

    public void add(Query query) {
        AbstractQueryImpl absQuery = ((AbstractQueryImpl) query);
        absQuery = absQuery.getRoot();
        if (!model.hasQuery(absQuery)) {
            model.addFirst(absQuery);

            //Init filters with default graph
            Graph graph = null;
            if (model != null && model.getGraphModel() != null) {
                graph = model.getGraphModel().getGraph();
            } else {
                GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
                graph = graphModel.getGraph();
            }

            for (Query q : query.getDescendantsAndSelf()) {
                Filter filter = q.getFilter();
                if (filter instanceof NodeFilter || filter instanceof EdgeFilter || filter instanceof AttributeFilter) {
                    FilterProcessor filterProcessor = new FilterProcessor();
                    filterProcessor.init(filter, graph);
                }
            }
        }
    }

    public void remove(Query query) {
        if (model.getCurrentQuery() == query) {
            if (model.isSelecting()) {
                selectVisible(null);
            } else {
                filterVisible(null);
            }
        }
        query = ((AbstractQueryImpl) query).getRoot();
        model.remove(query);
    }

    public void rename(Query query, String name) {
        model.rename(query, name);
    }

    public void setSubQuery(Query query, Query subQuery) {
        //Init subquery when new filter
        if (subQuery.getParent() == null && subQuery != model.getCurrentQuery()) {
            Graph graph = null;
            if (model != null && model.getGraphModel() != null) {
                graph = model.getGraphModel().getGraph();
            } else {
                GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
                graph = graphModel.getGraph();
            }
            Filter filter = subQuery.getFilter();
            if (filter instanceof NodeFilter || filter instanceof EdgeFilter || filter instanceof AttributeFilter) {
                FilterProcessor filterProcessor = new FilterProcessor();
                filterProcessor.init(filter, graph);
            }
        }
        
        model.setSubQuery(query, subQuery);
    }

    public void removeSubQuery(Query query, Query parent) {
        model.removeSubQuery(query, parent);
    }

    public void filterVisible(Query query) {
        if (query != null && model.getCurrentQuery() == query && model.isFiltering()) {
            return;
        }
        model.setFiltering(query != null);
        model.setCurrentQuery(query);

        if (model.getFilterThread() != null) {
            model.getFilterThread().setRunning(false);
            model.setFilterThread(null);
        }
        if (query != null) {
            FilterThread filterThread = new FilterThread(model);
            model.setFilterThread(filterThread);
            filterThread.setRootQuery((AbstractQueryImpl) query);
            filterThread.start();
        } else {
            model.getGraphModel().setVisibleView(null);
            if (model.getCurrentResult() != null) {
                model.getGraphModel().destroyView(model.getCurrentResult());
                model.setCurrentResult(null);
            }
        }
    }

    public GraphView filter(Query query) {
        FilterProcessor processor = new FilterProcessor();
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        Graph result = processor.process((AbstractQueryImpl) query, graphModel);
        return result.getView();
    }

    public void selectVisible(Query query) {
        if (query != null && model.getCurrentQuery() == query && model.isSelecting()) {
            return;
        }
        model.setSelecting(query != null);
        model.setCurrentQuery(query);

        if (model.getFilterThread() != null) {
            model.getFilterThread().setRunning(false);
            model.setFilterThread(null);
        }

        model.getGraphModel().setVisibleView(null);
        if (model.getCurrentResult() != null) {
            model.getGraphModel().destroyView(model.getCurrentResult());
            model.setCurrentResult(null);
        }

        if (query != null) {
            FilterThread filterThread = new FilterThread(model);
            model.setFilterThread(filterThread);
            filterThread.setRootQuery((AbstractQueryImpl) query);
            filterThread.start();
        } else {
            VisualizationController visController = Lookup.getDefault().lookup(VisualizationController.class);
            if (visController != null) {
                visController.selectNodes(null);
            }
        }
    }

    public void exportToColumn(String title, Query query) {
        Graph result;
        if (model.getCurrentQuery() == query) {
            GraphView view = model.getCurrentResult();
            if (view == null) {
                return;
            }
            result = model.getGraphModel().getGraph(view);
        } else {
            FilterProcessor processor = new FilterProcessor();
            GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
            result = (Graph) processor.process((AbstractQueryImpl) query, graphModel);
        }
//        AttributeModel am = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeModel am = Lookup.getDefault().lookup(AttributeModel.class);
//        AttributeColumn nodeCol = am.getNodeTable().getColumn("filter_" + title);
        Column nodeCol = am.getNodeTable().getColumn("filter_" + title);
        if (nodeCol == null) {
//            nodeCol = am.getNodeTable().addColumn("filter_" + title, title, AttributeType.BOOLEAN, AttributeOrigin.COMPUTED, Boolean.FALSE);
            // A guess: still working on this ...
            nodeCol = am.getNodeTable().addColumn("filter_" + title, title, null, Origin.DATA, am, true);
        }
        Column edgeCol = am.getEdgeTable().getColumn("filter_" + title);
        if (edgeCol == null) {
            edgeCol = am.getEdgeTable().addColumn("filter_" + title, title, AttributeType.BOOLEAN, AttributeOrigin.COMPUTED, Boolean.FALSE);
        }
        result.readLock();
        for (Node n : result.getNodes()) {
            n.setAttribute(nodeCol.getId(), Boolean.TRUE);
        }
        for (Edge e : result.getEdges()) {
            e.setAttribute(edgeCol.getId(), e);
        }
        result.readUnlock();
        //StatusDisplayer.getDefault().setStatusText(NbBundle.getMessage(FilterControllerImpl.class, "FilterController.exportToColumn.status", title));
    }

    public void exportToNewWorkspace(Query query) {
        Graph result;
        if (model.getCurrentQuery() == query) {
            GraphView view = model.getCurrentResult();
            if (view == null) {
                return;
            }
            result = model.getGraphModel().getGraph(view);
        } else {
            FilterProcessor processor = new FilterProcessor();
            GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
            result = (Graph) processor.process((AbstractQueryImpl) query, graphModel);
        }

        final Graph graphView = result;
        new Thread(new Runnable() {

            public void run() {
                ProgressTicketProvider progressProvider = Lookup.getDefault().lookup(ProgressTicketProvider.class);
                ProgressTicket ticket = null;
                if (progressProvider != null) {
                    ticket = progressProvider.createTicket("Export to workspace", null);
                }
                Progress.start(ticket);
                ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
                Workspace newWorkspace = pc.duplicateWorkspace(pc.getCurrentWorkspace());
                GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel(newWorkspace);
                // TODO: this is the part I'm not sure about
                graphModel.copyView(graphView.getView());
                Progress.finish(ticket);
                String workspaceName = newWorkspace.getLookup().lookup(WorkspaceInformation.class).getName();
                //StatusDisplayer.getDefault().setStatusText(NbBundle.getMessage(FilterControllerImpl.class, "FilterController.exportToNewWorkspace.status", workspaceName));
            }
        }, "Export filter to workspace").start();
    }

    public void exportToLabelVisible(Query query) {
        Graph result;
        if (model.getCurrentQuery() == query) {
            GraphView view = model.getCurrentResult();
            if (view == null) {
                return;
            }
            result = model.getGraphModel().getGraph(view);
        } else {
            FilterProcessor processor = new FilterProcessor();
            result = (Graph) processor.process((AbstractQueryImpl) query, model.getGraphModel());
        }
        Graph fullHGraph = model.getGraphModel().getGraph();
        fullHGraph.readLock();
        for (Node n : fullHGraph.getNodes()) {
            // TODO: test this
            boolean inView = n.getTextProperties().isVisible();
            n.getTextProperties().setVisible(inView);
        }
        for (Edge e : fullHGraph.getEdges()) {
            boolean inView = result.contains(e);
            e.getTextProperties().setVisible(inView);
        }
        fullHGraph.readUnlock();
    }

    public void setAutoRefresh(boolean autoRefresh) {
        if (model != null) {
            model.setAutoRefresh(autoRefresh);
        }
    }

    public void setCurrentQuery(Query query) {
        if (model != null) {
            model.setCurrentQuery(query);
        }
    }

    public FilterModel getGraphModel() {
        return model;
    }

    public synchronized FilterModel getGraphModel(Workspace workspace) {
        FilterModel filterModel = workspace.getLookup().lookup(FilterModel.class);
        if (filterModel == null) {
            filterModel = new FilterModelImpl(workspace);
            workspace.add(filterModel);
        }
        return filterModel;
    }

    public void setValue(FilterProperty property, Object value, Callback callback) {
        if (model != null) {
            Query query = model.getQuery(property.getFilter());
            if (query == null) {
                callback.setValue(value);
                return;
            }
            AbstractQueryImpl rootQuery = ((AbstractQueryImpl) query).getRoot();
            FilterThread filterThread = null;
            if ((filterThread = model.getFilterThread()) != null && model.getCurrentQuery() == rootQuery) {
                if (Thread.currentThread().equals(filterThread)) {
                    //Called inside of the thread, in init for instance. Update normally.
                    callback.setValue(value);
                    model.updateParameters(query);
                } else {
                    //The query is currently being filtered by the thread, or finished to do it
                    filterThread.addModifier(new PropertyModifier(query, property, value, callback));
                    filterThread.setRootQuery(rootQuery);
                }
            } else {
                //Update normally
                callback.setValue(value);
                model.updateParameters(query);
            }
        } else {
            callback.setValue(value);
        }
    }

    @Override
    public FilterModel getModel() {
        return model;
    }

    @Override
    public FilterModel getModel(Workspace workspace) {
        return model;
    }
}
