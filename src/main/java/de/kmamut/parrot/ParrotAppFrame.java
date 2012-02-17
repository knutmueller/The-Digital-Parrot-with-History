package de.kmamut.parrot;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModelFactory.Style;
import net.schweerelos.parrot.ui.AbstractHistoryNavigator;
import net.schweerelos.parrot.ui.HistoryNavigatorHelper;
import net.schweerelos.parrot.ui.MainViewComponent;
import net.schweerelos.parrot.ui.NavigatorComponent;
import net.schweerelos.parrot.ui.ParrotStateListener;
import net.schweerelos.parrot.ui.PickListener;
import net.schweerelos.parrot.ui.UnknownStyleException;
import net.schweerelos.parrot.ui.UserInterfaceManager;
import javax.swing.JPanel;

public class ParrotAppFrame extends JFrame {

	private static final Logger LOG = Logger.getLogger(ParrotAppFrame.class);
	
	private static final long serialVersionUID = 1L;
	private final String APP_TITLE = "The Digital Parrot with History";
	private final int MAIN_VIEW_COUNT = 1;
	private final int NAVIGATOR_COUNT = 4;

	private MainViewComponent activeMainView;
	private List<MainViewComponent> mainViews = new ArrayList<MainViewComponent>(MAIN_VIEW_COUNT);
	private List<NavigatorComponent> navigators = new ArrayList<NavigatorComponent>(NAVIGATOR_COUNT);
	private Map<Window, Point> preferredFrameLocations = new HashMap<Window, Point>();
	
	public ParrotAppFrame(Properties properties) {
		super();
		LOG.setLevel(Level.ALL);
		setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
		
		setTitle(APP_TITLE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				for (MainViewComponent mainViewComponent : mainViews) {	
					if (mainViewComponent instanceof ParrotStateListener) {
						((ParrotStateListener) mainViewComponent).parrotExiting();
					}
				}
			}
		});
		setSize(920, 690);

		getContentPane().setLayout(new BorderLayout());

		UserInterfaceManager uiManager = new UserInterfaceManager(properties);

		LOG.debug("Create main view.");
		// main view
		final JTabbedPane mainPanel = new JTabbedPane();
		try {
			mainViews.add(uiManager.createMainViewComponent(Style.GRAPH));
//			mainViews.add(uiManager.createMainViewComponent(Style.TABLE));
		} catch (UnknownStyleException e) {
			Logger.getLogger("ParrotApp").error("UnknownStyleException: " + e.getMessage(), e);
			System.exit(ERROR);
		}
		for (MainViewComponent mainView : mainViews)
			mainPanel.add(mainView.getTitle(), mainView.asJComponent());
		mainPanel.setSelectedIndex(0);
		activeMainView = mainViews.get(0);
		
		mainPanel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (e.getSource() != mainPanel) {
					return;
				}
				int selectedIndex = mainPanel.getSelectedIndex();
				activeMainView = mainViews.get(selectedIndex);
				activeMainView.asJComponent().setVisible(true);
			}
		});
		getContentPane().add(mainPanel, BorderLayout.CENTER);
		
		// navigators
		JToolBar navigatorsBar = new JToolBar(JToolBar.HORIZONTAL);
		navigatorsBar.setMargin(new Insets(0, 11, 0, 0));
		navigatorsBar.setFloatable(false);
		getContentPane().add(navigatorsBar, BorderLayout.PAGE_START);

		LOG.debug("Create timeline navigator.");
		// timeline
		NavigatorComponent timelineNavigator = uiManager.createTimelineNavigationComponent();
		navigators.add(timelineNavigator);

		JFrame timelineFrame = new JFrame(timelineNavigator.getNavigatorName() + " – " + APP_TITLE);

		timelineFrame.getContentPane().add(timelineNavigator.asJComponent());
		timelineFrame.pack();
		Point preferredLocation = new Point(0,0);
		preferredFrameLocations.put(timelineFrame, preferredLocation);

		if (timelineNavigator.hasShowHideListener()) {
			timelineFrame.addComponentListener(timelineNavigator.getShowHideListener());
		}
		timelineFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		JToggleButton timelineButton = setupNavigatorButton(timelineNavigator);
		navigatorsBar.add(timelineButton);

		LOG.debug("Create map navigator.");
		// map		
//		NavigatorComponent mapNavigator = uiManager.createMapNavigationComponent();
//		navigators.add(mapNavigator);
//
//		JFrame mapFrame = new JFrame(mapNavigator.getNavigatorName() + " – " + APP_TITLE);
//
//		mapFrame.getContentPane().add(mapNavigator.asJComponent());
//		mapFrame.pack();
//		preferredLocation = new Point(0, Toolkit.getDefaultToolkit().getScreenSize().height - mapFrame.getHeight());
//		preferredFrameLocations.put(mapFrame, preferredLocation);
//
//		if (mapNavigator.hasShowHideListener()) {
//			mapFrame.addComponentListener(mapNavigator.getShowHideListener());
//		}
//		mapFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
//
//		JToggleButton mapButton = setupNavigatorButton(mapNavigator);
//		navigatorsBar.add(mapButton);

		LOG.debug("Create search navigator.");
		// search		
		NavigatorComponent searchNavigator = uiManager.createSearchComponent();
		navigators.add(searchNavigator);

		JFrame searchFrame = new JFrame(searchNavigator.getNavigatorName() + " – " + APP_TITLE);

		searchFrame.getContentPane().add(searchNavigator.asJComponent());
		searchFrame.pack();
		preferredLocation = new Point(Toolkit.getDefaultToolkit().getScreenSize().width - searchFrame.getWidth(), 0);
		preferredFrameLocations.put(searchFrame, preferredLocation);

		if (searchNavigator.hasShowHideListener()) {
			searchFrame.addComponentListener(searchNavigator.getShowHideListener());
		}
		searchFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		JToggleButton searchButton = setupNavigatorButton(searchNavigator);
		navigatorsBar.add(searchButton);

		// bottomPanel
		JPanel bottomPanel = new JPanel(new BorderLayout(0,0));
		getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		
		LOG.debug("Create connections navigator.");
		// connections
		NavigatorComponent chainNavigator = uiManager.createChainNavigationComponent();
		navigators.add(chainNavigator);

		if (chainNavigator instanceof PickListener) {
			for (MainViewComponent mainViewComponent : mainViews) {	
				mainViewComponent.addPickListener((PickListener) chainNavigator);
			}
		}

		if (chainNavigator.hasShowHideListener()) {
			chainNavigator.asJComponent().addComponentListener(chainNavigator.getShowHideListener());
		}

		JToggleButton connectionsButton = setupNavigatorButton(chainNavigator);
		navigatorsBar.add(connectionsButton);

		bottomPanel.add(chainNavigator.asJComponent(), BorderLayout.SOUTH);
		chainNavigator.asJComponent().setVisible(false);
		
		LOG.debug("Create history navigator.");
		// history navigator helper; needed to make sure that all history navigators operate on same data
		HistoryNavigatorHelper helper = new HistoryNavigatorHelper();
		
		LOG.debug("Create history slider navigator.");
		// history slider
		NavigatorComponent historySliderNavigator = uiManager.createHistorySliderNavigationComponent(helper);
		navigators.add(historySliderNavigator);
		
		if (historySliderNavigator.hasShowHideListener()) {
			historySliderNavigator.asJComponent().addComponentListener(historySliderNavigator.getShowHideListener());
		}
		
		JToggleButton historySliderButton = setupNavigatorButton(historySliderNavigator);
		navigatorsBar.add(historySliderButton);
		
		bottomPanel.add(historySliderNavigator.asJComponent(), BorderLayout.NORTH);
		historySliderNavigator.asJComponent().setVisible(false);
		
		LOG.debug("Create history list navigator.");
		// history list
		NavigatorComponent historyListNavigator = uiManager.createHistoryListNavigationComponent(helper);
		navigators.add(historyListNavigator);
		
		if (historyListNavigator.hasShowHideListener()) {
			historyListNavigator.asJComponent().addComponentListener(historyListNavigator.getShowHideListener());
		}

		JToggleButton historyListButton = setupNavigatorButton(historyListNavigator);
		navigatorsBar.add(historyListButton);
		
		getContentPane().add(historyListNavigator.asJComponent(), BorderLayout.EAST);
		historyListNavigator.asJComponent().setVisible(false);

	}

	private JToggleButton setupNavigatorButton(final NavigatorComponent navigator) {
		final String name = navigator.getNavigatorName();
		final String accelerator = navigator.getAcceleratorKey();
		
		final Component component = navigator.asJComponent();
		AbstractAction showNavigatorAction = new AbstractAction(name) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!(e.getSource() instanceof JToggleButton)) {
					return;
				}
				final Window window;
				if (component instanceof Window) {
					window = (Window) component;
				} else {
					window = SwingUtilities.getWindowAncestor(component);
				}
				JToggleButton button = (JToggleButton) e.getSource();
				boolean show = button.isSelected();
				if (show) {
					if (window != ParrotAppFrame.this && preferredFrameLocations.containsKey(window)) {
						window.setLocation(preferredFrameLocations.get(window));
					}
				}
				if (navigator.tellSelectionWhenShown()) {
					Collection<NodeWrapper> selectedNodes = activeMainView.getSelectedNodes();
					navigator.setSelectedNodes(selectedNodes);
				}
				if (show) {
					window.setVisible(true);
				} else if (window != ParrotAppFrame.this) {
					window.setVisible(false);
				}
				component.setVisible(show);
			}
		};
		showNavigatorAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control " + accelerator));
		final JToggleButton button = new JToggleButton(showNavigatorAction);
		button.setToolTipText("Show " + name.toLowerCase());
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				button.setToolTipText((button.isSelected() ? "Hide " : "Show ") + name.toLowerCase());
			}
		});
		final Window window;
		if (component instanceof Window) {
			window = (Window) component;
		} else {
			window = SwingUtilities.getWindowAncestor(component);
		}
		if (window != null) {
			window.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					button.setSelected(false);
					if (window != ParrotAppFrame.this) {
						preferredFrameLocations.put(window, window.getLocation());	
					}
				}
				@Override
				public void componentShown(ComponentEvent e) {
					button.setSelected(true);
				}
			});
		}
		if (component instanceof AbstractHistoryNavigator) {
			component.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					button.setSelected(false);
				}
				@Override
				public void componentShown(ComponentEvent e) {
					button.setSelected(true);
				}
			});
		}
		return button;
	}

	public List<MainViewComponent> getMainViews() {
		return mainViews;
	}

	public List<NavigatorComponent> getNavigators() {
		return navigators;
	}
	
	public Map<String, List<Action>> getNavigatorActionsForNode(NodeWrapper node) {
		Map<String, List<Action>> map = new HashMap<String, List<Action>>();
		for (NavigatorComponent navigator : navigators) {
			map.put(navigator.getNavigatorName(), navigator.getActionsForNode(node));
		}
		return map;
	}

	public Map<String, List<Action>> getNavigatorActionsForType(NodeWrapper type) {
		Map<String, List<Action>> map = new HashMap<String, List<Action>>();
		for (NavigatorComponent navigator : navigators) {
			map.put(navigator.getNavigatorName(), navigator.getActionsForType(type));
		}
		return map;
	}




}
