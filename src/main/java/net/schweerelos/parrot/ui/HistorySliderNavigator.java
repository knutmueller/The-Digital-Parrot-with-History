package net.schweerelos.parrot.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import de.kmamut.parrot.history.changes.Version;

public class HistorySliderNavigator extends AbstractHistoryNavigator implements NavigatorComponent {

	private static final long serialVersionUID = 1L;
	
	private static final String NAME = "History Slider";
	private static final String ACCELERATOR_KEY = "H";

	private JButton backwardButton;
	private JButton forwardButton;
	private JSlider historySlider;

	@Override
	public String getNavigatorName() {
		return NAME;
	}

	@Override
	public String getAcceleratorKey() {
		return ACCELERATOR_KEY;
	}

	public HistorySliderNavigator(HistoryNavigatorHelper helper) {
		super(NAME, helper);
		setLayout(new BorderLayout());
	}

	@Override
	protected void initialise() {
		backwardButton = new JButton(new AbstractAction("<") {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				historySlider.setValue(historySlider.getValue() - 1);
			}
		});
		
		forwardButton = new JButton(new AbstractAction(">") {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				historySlider.setValue(historySlider.getValue() + 1);
			}
		});
		forwardButton.setEnabled(false);
		
		DateTimeFormatter labelFormatter = new DateTimeFormatterBuilder()
			.appendLiteral("<html><center>")
			.appendDayOfMonth(1)
			.appendLiteral(' ')
			.appendMonthOfYearShortText()
			.appendLiteral("<br/>")
			.appendYear(4, 4)
			.appendLiteral("</center></html>")
			.toFormatter();
		int i = -1;
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		for (Version version : getVersions()) {
			String label = labelFormatter.print(version.getDateTime());
			labelTable.put(++i, new JLabel(label));
		}
		historySlider = new JSlider(0, i, i);
		historySlider.setLabelTable(labelTable);
		historySlider.setPaintLabels(true);
		historySlider.setMajorTickSpacing(1);
		historySlider.setSnapToTicks(true);
		historySlider.setPaintTicks(true);
		historySlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider slider = (JSlider)e.getSource();
				int state = slider.getValue();
		        if (!slider.getValueIsAdjusting())
		        	setState(state);
		        backwardButton.setEnabled(state != slider.getMinimum());
				forwardButton.setEnabled(state != slider.getMaximum());
			}
		});
		
		// add buttons and slider to panel
		add(backwardButton, BorderLayout.WEST);
		add(forwardButton, BorderLayout.EAST);
		add(historySlider, BorderLayout.CENTER);
		
		// history state change listener
		super.addHistoryStateListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				historySlider.setValue((Integer) e.getNewValue());
			}
		});
	}

	@Override
	protected void resetLabelsColor(Color color) {
		for (int state = 0; state < historySlider.getLabelTable().size(); state++)
			setLabelColor(state, color);
	}

	@Override
	protected void setLabelColor(Integer state, Color color) {
		if (state != null)
			((JLabel) historySlider.getLabelTable().get(state)).setForeground(color);
	}

}
