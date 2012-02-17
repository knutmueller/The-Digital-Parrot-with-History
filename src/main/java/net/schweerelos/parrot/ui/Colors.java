package net.schweerelos.parrot.ui;

import java.awt.Color;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.VersionedNodeWrapper;

import de.kmamut.parrot.history.changes.ChangeType;

public enum Colors { 
		
	BORDER, LINE, TEXT, LABEL, BACKGROUND,
	PICKED, HIGHLIGHTED,
	NEIGHBOUR, ADJACENT;

	private static final EnumMap<ChangeType, Map<EnumSet<Colors>, Color>> nodeColorMap = createNodeColorMap();
	private static final EnumMap<ChangeType, Map<EnumSet<Colors>, Color>> createNodeColorMap() {
		EnumMap<ChangeType, Map<EnumSet<Colors>, Color>> typedMapping = new EnumMap<ChangeType, Map<EnumSet<Colors>,Color>>(ChangeType.class);
		Map<EnumSet<Colors>, Color> defaultMap = new HashMap<EnumSet<Colors>, Color>();
		
		defaultMap.put(EnumSet.of(TEXT), UIConstants.TT_TEXT);
		defaultMap.put(EnumSet.of(TEXT, PICKED), UIConstants.TEXT);
		defaultMap.put(EnumSet.of(TEXT, HIGHLIGHTED), UIConstants.TEXT);
		defaultMap.put(EnumSet.of(TEXT, PICKED, NEIGHBOUR), UIConstants.T_TEXT);
		defaultMap.put(EnumSet.of(TEXT, HIGHLIGHTED, NEIGHBOUR), UIConstants.T_TEXT);
		defaultMap.put(EnumSet.of(TEXT, ADJACENT, PICKED), UIConstants.T_TEXT);
		
		defaultMap.put(EnumSet.of(BACKGROUND), UIConstants.TT_ENVIRONMENT_LIGHT);
		defaultMap.put(EnumSet.of(BACKGROUND, PICKED), UIConstants.ACCENT_LIGHT);
		defaultMap.put(EnumSet.of(BACKGROUND, HIGHLIGHTED), UIConstants.ENVIRONMENT_LIGHTEST);
		defaultMap.put(EnumSet.of(BACKGROUND, PICKED, NEIGHBOUR), UIConstants.ACCENT_LIGHTEST);
		defaultMap.put(EnumSet.of(BACKGROUND, HIGHLIGHTED, NEIGHBOUR), UIConstants.T_ENVIRONMENT_LIGHTEST);
		defaultMap.put(EnumSet.of(BACKGROUND, ADJACENT, PICKED), UIConstants.ACCENT_LIGHTEST);
		
		defaultMap.put(EnumSet.of(BORDER), UIConstants.TT_ENVIRONMENT_MEDIUM);
		defaultMap.put(EnumSet.of(BORDER, PICKED), UIConstants.ACCENT_MEDIUM);
		defaultMap.put(EnumSet.of(BORDER, HIGHLIGHTED), UIConstants.ENVIRONMENT_SHADOW_DARK);
		defaultMap.put(EnumSet.of(BORDER, PICKED, NEIGHBOUR), UIConstants.T_ACCENT_MEDIUM);
		defaultMap.put(EnumSet.of(BORDER, HIGHLIGHTED, NEIGHBOUR), UIConstants.ENVIRONMENT_SHADOW_DARK);
		defaultMap.put(EnumSet.of(BORDER, ADJACENT, PICKED), UIConstants.ACCENT_MEDIUM);
		
		typedMapping.put(ChangeType.UNTOUCHED, defaultMap);
		typedMapping.put(ChangeType.ADDITION, new HashMap<EnumSet<Colors>, Color>(defaultMap));
		typedMapping.put(ChangeType.REMOVAL, new HashMap<EnumSet<Colors>, Color>(defaultMap));
		typedMapping.put(ChangeType.CHANGE, new HashMap<EnumSet<Colors>, Color>(defaultMap));
		
		typedMapping.get(ChangeType.ADDITION).put(EnumSet.of(BORDER), UIConstants.TT_ADDITION);
		typedMapping.get(ChangeType.ADDITION).put(EnumSet.of(BORDER, PICKED), UIConstants.ADDITION);
		typedMapping.get(ChangeType.ADDITION).put(EnumSet.of(BORDER, HIGHLIGHTED), UIConstants.ADDITION);
		typedMapping.get(ChangeType.ADDITION).put(EnumSet.of(BORDER, PICKED, NEIGHBOUR), UIConstants.T_ADDITION);
		typedMapping.get(ChangeType.ADDITION).put(EnumSet.of(BORDER, HIGHLIGHTED, NEIGHBOUR), UIConstants.T_ADDITION);
		typedMapping.get(ChangeType.ADDITION).put(EnumSet.of(BORDER, ADJACENT, PICKED), UIConstants.ADDITION);
		
		typedMapping.get(ChangeType.REMOVAL).put(EnumSet.of(BORDER), UIConstants.TT_REMOVAL);
		typedMapping.get(ChangeType.REMOVAL).put(EnumSet.of(BORDER, PICKED), UIConstants.REMOVAL);
		typedMapping.get(ChangeType.REMOVAL).put(EnumSet.of(BORDER, HIGHLIGHTED), UIConstants.REMOVAL);
		typedMapping.get(ChangeType.REMOVAL).put(EnumSet.of(BORDER, PICKED, NEIGHBOUR), UIConstants.T_REMOVAL);
		typedMapping.get(ChangeType.REMOVAL).put(EnumSet.of(BORDER, HIGHLIGHTED, NEIGHBOUR), UIConstants.T_REMOVAL);
		typedMapping.get(ChangeType.REMOVAL).put(EnumSet.of(BORDER, ADJACENT, PICKED), UIConstants.REMOVAL);
		
		typedMapping.get(ChangeType.CHANGE).put(EnumSet.of(BORDER), UIConstants.TT_CHANGE);
		typedMapping.get(ChangeType.CHANGE).put(EnumSet.of(BORDER, PICKED), UIConstants.CHANGE);
		typedMapping.get(ChangeType.CHANGE).put(EnumSet.of(BORDER, HIGHLIGHTED), UIConstants.CHANGE);
		typedMapping.get(ChangeType.CHANGE).put(EnumSet.of(BORDER, PICKED, NEIGHBOUR), UIConstants.T_CHANGE);
		typedMapping.get(ChangeType.CHANGE).put(EnumSet.of(BORDER, HIGHLIGHTED, NEIGHBOUR), UIConstants.T_CHANGE);
		typedMapping.get(ChangeType.CHANGE).put(EnumSet.of(BORDER, ADJACENT, PICKED), UIConstants.CHANGE);
		
		return typedMapping;
	}
	
	private static final EnumMap<ChangeType, Map<EnumSet<Colors>, Color>> edgeColorMap = createEdgeColorMap();
	private static final EnumMap<ChangeType, Map<EnumSet<Colors>, Color>> createEdgeColorMap() {
		EnumMap<ChangeType, Map<EnumSet<Colors>, Color>> typedMapping = new EnumMap<ChangeType, Map<EnumSet<Colors>,Color>>(ChangeType.class);
		Map<EnumSet<Colors>, Color> defaultMap = new HashMap<EnumSet<Colors>, Color>();
		
		defaultMap.put(EnumSet.of(LABEL), UIConstants.TEXT);
		
		defaultMap.put(EnumSet.of(LINE), getNodeColor(ChangeType.UNTOUCHED, EnumSet.of(BORDER)));
		defaultMap.put(EnumSet.of(LINE, PICKED), getNodeColor(ChangeType.UNTOUCHED, EnumSet.of(BORDER, PICKED)));
		defaultMap.put(EnumSet.of(LINE, HIGHLIGHTED), getNodeColor(ChangeType.UNTOUCHED, EnumSet.of(BORDER, HIGHLIGHTED)));
		defaultMap.put(EnumSet.of(LINE, ADJACENT, PICKED), getNodeColor(ChangeType.UNTOUCHED, EnumSet.of(BORDER, PICKED)));
		defaultMap.put(EnumSet.of(LINE, ADJACENT, HIGHLIGHTED), getNodeColor(ChangeType.UNTOUCHED, EnumSet.of(BORDER, HIGHLIGHTED)));
		
		typedMapping.put(ChangeType.UNTOUCHED, defaultMap);
		typedMapping.put(ChangeType.ADDITION, new HashMap<EnumSet<Colors>, Color>(defaultMap));
		typedMapping.put(ChangeType.REMOVAL, new HashMap<EnumSet<Colors>, Color>(defaultMap));
		typedMapping.put(ChangeType.CHANGE, new HashMap<EnumSet<Colors>, Color>(defaultMap));
		
		typedMapping.get(ChangeType.ADDITION).put(EnumSet.of(LINE), getNodeColor(ChangeType.ADDITION, EnumSet.of(BORDER)));
		typedMapping.get(ChangeType.ADDITION).put(EnumSet.of(LINE, PICKED), getNodeColor(ChangeType.ADDITION, EnumSet.of(BORDER, PICKED)));
		typedMapping.get(ChangeType.ADDITION).put(EnumSet.of(LINE, HIGHLIGHTED), getNodeColor(ChangeType.ADDITION, EnumSet.of(BORDER, HIGHLIGHTED)));
		typedMapping.get(ChangeType.ADDITION).put(EnumSet.of(LINE, ADJACENT, PICKED), getNodeColor(ChangeType.ADDITION, EnumSet.of(BORDER, PICKED)));
		typedMapping.get(ChangeType.ADDITION).put(EnumSet.of(LINE, ADJACENT, HIGHLIGHTED), getNodeColor(ChangeType.ADDITION, EnumSet.of(BORDER, HIGHLIGHTED)));
		
		typedMapping.get(ChangeType.REMOVAL).put(EnumSet.of(LINE), getNodeColor(ChangeType.REMOVAL, EnumSet.of(BORDER)));
		typedMapping.get(ChangeType.REMOVAL).put(EnumSet.of(LINE, PICKED), getNodeColor(ChangeType.REMOVAL, EnumSet.of(BORDER, PICKED)));
		typedMapping.get(ChangeType.REMOVAL).put(EnumSet.of(LINE, HIGHLIGHTED), getNodeColor(ChangeType.REMOVAL, EnumSet.of(BORDER, HIGHLIGHTED)));
		typedMapping.get(ChangeType.REMOVAL).put(EnumSet.of(LINE, ADJACENT, PICKED), getNodeColor(ChangeType.REMOVAL, EnumSet.of(BORDER, PICKED)));
		typedMapping.get(ChangeType.REMOVAL).put(EnumSet.of(LINE, ADJACENT, HIGHLIGHTED), getNodeColor(ChangeType.REMOVAL, EnumSet.of(BORDER, HIGHLIGHTED)));
		
		typedMapping.get(ChangeType.CHANGE).put(EnumSet.of(LINE), getNodeColor(ChangeType.CHANGE, EnumSet.of(BORDER)));
		typedMapping.get(ChangeType.CHANGE).put(EnumSet.of(LINE, PICKED), getNodeColor(ChangeType.CHANGE, EnumSet.of(BORDER, PICKED)));
		typedMapping.get(ChangeType.CHANGE).put(EnumSet.of(LINE, HIGHLIGHTED), getNodeColor(ChangeType.CHANGE, EnumSet.of(BORDER, HIGHLIGHTED)));
		typedMapping.get(ChangeType.CHANGE).put(EnumSet.of(LINE, ADJACENT, PICKED), getNodeColor(ChangeType.CHANGE, EnumSet.of(BORDER, PICKED)));
		typedMapping.get(ChangeType.CHANGE).put(EnumSet.of(LINE, ADJACENT, HIGHLIGHTED), getNodeColor(ChangeType.CHANGE, EnumSet.of(BORDER, HIGHLIGHTED)));
		
		return typedMapping;
	}
	
	private static ChangeType extractChangeType(NodeWrapper wrapper) {
		if (wrapper instanceof VersionedNodeWrapper) {
			if (((VersionedNodeWrapper) wrapper).isAdded())
				return ChangeType.ADDITION;
			if (((VersionedNodeWrapper) wrapper).isRemoved())
				return ChangeType.REMOVAL;
			if (((VersionedNodeWrapper) wrapper).isChanged())
				return ChangeType.CHANGE;
		}
		return ChangeType.UNTOUCHED;
	}

	private static final Color getNodeColor(ChangeType type, EnumSet<Colors> selectors) {
		return nodeColorMap.get(type).get(selectors);
	}
	
	public static final Color getNodeColor(EnumSet<Colors> selectors) {
		return getNodeColor(ChangeType.UNTOUCHED, selectors);
	}
	public static final Color getNodeColor(NodeWrapper wrapper, EnumSet<Colors> selectors) {
		ChangeType type = extractChangeType(wrapper);
		return getNodeColor(type, selectors);
	}
	
	private static final Color getEdgeColor(ChangeType type, EnumSet<Colors> selectors) {
		return edgeColorMap.get(type).get(selectors);
	}
	
	public static final Color getEdgeColor(EnumSet<Colors> selectors) {
		return getEdgeColor(ChangeType.UNTOUCHED, selectors);
	}
	public static final Color getEdgeColor(NodeWrapper wrapper, EnumSet<Colors> selectors) {
		ChangeType type = extractChangeType(wrapper);
		return getEdgeColor(type, selectors);
	}
	
	public static final Color getBackgroundColor() {
		return Color.WHITE;
	}

}