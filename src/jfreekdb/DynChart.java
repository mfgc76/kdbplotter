package jfreekdb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.table.AbstractTableModel;

import net.sf.jtreemap.swing.JTreeMap;
import net.sf.jtreemap.swing.TreeMapNode;
import net.sf.jtreemap.swing.TreeMapNodeBuilder;
import net.sf.jtreemap.swing.ValuePercent;
import net.sf.jtreemap.swing.provider.HSBTreeMapColorProvider;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.OHLCDataItem;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;

import kx.c;
import kx.c.KException;
import kx.c.Minute;
import kx.c.Second;


// http://stackoverflow.com/questions/5522575
public class DynChart { //extends TimerTask{
	
	Timer timer;
	private static final String title = "kdbplotter";
	static JFreeChart chart1;
	static JTreeMap 	map1;
	static JFrame f = new JFrame(title);
	static JPanel view = new JPanel();
    static JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	
	public static int rowCount=0;
	public static int columnCount=0;
    static int nrow=0;
    static int ncol=0;
	public static c.Flip flip; // query result is table
	
    private static ChartPanel chartPanel = createChart();
    private static int chartType=0;
    private static String Host=":5600";
    private static String Query="select from tab";
    private static int RefreshTime=0;
    JTextField edit_refresh = new JTextField();
    JTextField edit_query = new JTextField();
    JTextField edit_host = new JTextField();
    JCheckBox chk_refresh = new JCheckBox();
    JComboBox cmb_ctype = new JComboBox();

    class ToDoTask extends TimerTask  {
        public void run (  )   {
            timer.cancel () ; //Terminate the thread
            if((RefreshTime>0)&&(chk_refresh.isEnabled()==true)){
        		timer = new Timer();
        		timer.schedule(new ToDoTask(), RefreshTime*1000); //initial delay, period 
        	}
    		if(cmb_ctype.getSelectedIndex()!=6) { //because jtreemap is different from jfreechart must destroy panel
				f.remove(view);
    			f.add(chartPanel, BorderLayout.CENTER);f.validate();
    			chart1=buildChart();
	            chart1.removeLegend();
	            chartPanel.setChart(chart1);
	            chartPanel.updateUI();
    		} else {
    			buildChart();
    			f.remove(chartPanel);
    			f.add(view, BorderLayout.CENTER);f.validate();
    		}
/*          chart1=buildChart();
          chart1.removeLegend();
          chartPanel.setChart(chart1);
          chartPanel.updateUI();
  */      }
      }
    public DynChart() {
    	
        f.setTitle(title);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLayout(new BorderLayout(0, 5));

        f.add(view, BorderLayout.CENTER);
		f.add(chartPanel, BorderLayout.CENTER);
		  
        //f.add(chartPanel, BorderLayout.CENTER);  //add treemap
        chartPanel.setMouseWheelEnabled(true);
        //chartPanel.setHorizontalAxisTrace(true);
        //chartPanel.setVerticalAxisTrace(true);
        
 //       panel.add(createTrace());
        panel.add(new JLabel("host:"));
        panel.add(createHost());
        panel.add(new JLabel("query:"));
        panel.add(createQuery());
        panel.add(createRefreshTick());
        panel.add(new JLabel("refresh(s):"));
        panel.add(createRefresh());
        panel.add(createChartType());
        panel.add(createPlot());

        f.add(panel, BorderLayout.SOUTH);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
/*
    private JComboBox createTrace() {
        final JComboBox trace = new JComboBox();
        final String[] traceCmds = {"Enable Trace", "Disable Trace"};
        trace.setModel(new DefaultComboBoxModel(traceCmds));
        trace.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (traceCmds[0].equals(trace.getSelectedItem())) {
                    chartPanel.setHorizontalAxisTrace(true);
                    chartPanel.setVerticalAxisTrace(true);
                    chartPanel.repaint();
                } else {
                    chartPanel.setHorizontalAxisTrace(false);
                    chartPanel.setVerticalAxisTrace(false);
                    chartPanel.repaint();
                }
            }
        });
        return trace;
    }
   */ 
    //point: point/line/bar-1 or 2 cols
    //timeseries: timestamp-value
    //candlestick:ohlcv/ohlc
    //bubble:4cols - eg. sym,pos,prc,vol
    
    //refresh(s):
    //query
    //host:port:user:pass
    //axis names, title/legend?
    //tickbox,query btn
    private JComboBox createChartType() {
        final String[] ctypes = {"point","line","bar","timeseries","candlestick","bubble","treemap"};
        cmb_ctype.setModel(new DefaultComboBoxModel(ctypes));
        cmb_ctype.setSelectedIndex(3);
     /*   cmb_ctype.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
            	chartType=cmb_ctype.getSelectedIndex();
            	
            	chart1=buildChart();
            	chart1.removeLegend();
        		chartPanel.setChart(chart1);
            	//chart1.setBackgroundPaint(Color.DARK_GRAY);
        		chartPanel.updateUI();
           }
        });
    */    return cmb_ctype;
    }

    //set timer http://www.rgagnon.com/javadetails/java-0144.html
    private JButton createPlot() {
        final JButton btn_sendqry = new JButton("Plot");
        
        btn_sendqry.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	RefreshTime=Integer.parseInt((edit_refresh.getText()));
            	Query=edit_query.getText();
            	Host=edit_host.getText();
            	chartType=cmb_ctype.getSelectedIndex();
            	if((RefreshTime>0)&&(chk_refresh.isEnabled()==true)){
                	if(timer!=null) timer.cancel();
            		timer = new Timer();
            		timer.schedule(new ToDoTask(), RefreshTime*1000); //initial delay=period, period
            	}
            		if(cmb_ctype.getSelectedIndex()!=6) {
        				f.remove(view);
            			f.add(chartPanel, BorderLayout.CENTER);f.validate();
            			chart1=buildChart();
    		            chart1.removeLegend();
    		            chartPanel.setChart(chart1);
    		            chartPanel.updateUI();
            		} else {
            			buildChart();
            			f.remove(chartPanel);
            			f.add(view, BorderLayout.CENTER);f.validate();
            		}
               /* 	chart1=buildChart();
		            chart1.removeLegend();
		            chartPanel.setChart(chart1);
		            chartPanel.updateUI();
		            */
            }
        });
        return btn_sendqry;
    }
    
    private JTextField createQuery() {
        edit_query.setText(Query);
        edit_query.setColumns(30);
        return edit_query;
    }
    private JCheckBox createRefreshTick(){
    	chk_refresh.setText("refresh");
    	chk_refresh.setEnabled(true);
    	   chk_refresh.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
               	if((chk_refresh.isSelected()==false)&&(timer!=null)) timer.cancel();
              }
           });
		return chk_refresh;
    } 
    private JTextField createRefresh() {
        edit_refresh.setText(Integer.toString(RefreshTime));
        edit_refresh.setColumns(3); //edit field size
        edit_refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	try{
            	 RefreshTime=Integer.parseInt(edit_refresh.getText());
            	} finally{}
            }
        });
        return edit_refresh;
    }
    private JTextField createHost() {
        edit_host.setText(Host);
        edit_host.setColumns(3);
        return edit_host;
    }
    
    private static ChartPanel createChart() {

	    	chart1=buildChart(); 
	    	//chart1.setBackgroundPaint(Color.DARK_GRAY);
	    	//chart1.setBackgroundPaint(Color.DARK_GRAY);
	    	chart1.removeLegend();
	    
	    	return new ChartPanel(chart1);
    	
    }
    
/*
    private static double getLowestLow(DefaultCategoryDataset dataset){
        double lowest;
        lowest = (double) dataset.getValue(0,0);
        for(int i=1;i<dataset.getRowCount();i++){
            if((double) dataset.getValue(0, i) < lowest){
                lowest = (double) dataset.getValue(0,i);
            }
        }

        return lowest;
    }


    private static double getHighestHigh(DefaultCategoryDataset dataset){
        double highest;
        highest = (double) dataset.getValue(0,0);
        for(int i=1;i<dataset.getRowCount();i++){
            if((double) dataset.getValue(0, i) > highest){
                highest = (double) dataset.getValue(0,i);
            }
        }

        return highest;
    }   
*/
    //http://code.kx.com/wiki/Cookbook/InterfacingWithJava#Example_Grid_Viewer_using_Swing
    public static class KxTableModel extends AbstractTableModel {
     
    	private static final long serialVersionUID = 1L; //to remove serial warning
		private c.Flip flip;
        public void setFlip(c.Flip data) {
            this.flip = data;
        }
        public int getRowCount() {
            return Array.getLength(flip.y[0]);
        }
        public int getColumnCount() {
            return flip.y.length;
        }
        public Object getValueAt(int rowIndex, int columnIndex) {
            return c.at(flip.y[columnIndex], rowIndex);
        }
        public String getColumnName(int columnIndex) {
            return flip.x[columnIndex];
        }
    };

    public static Object[][] getSeries(String Host,String Query){
    	KxTableModel model = new KxTableModel();
        c c = null;
        String lhost;
        String hoststr[];
        String user,pass;
        Object [][] tmp3;

        if(Query!=null){
	            hoststr=Host.split(":");  //split at ":"  hostname:port:(username:password), user,passwd are optional
		        
		        if(hoststr[0].equals("")) lhost="localhost";
		        else lhost=hoststr[0];
		        int lport=Integer.parseInt(hoststr[1]);
		        try {
	                    if(hoststr.length==4){
	                        user=hoststr[2];
	                        pass=hoststr[3];
	                        c = new c(lhost,lport,(user+":"+pass));
	                    }
	                    else{
	                        c = new c(lhost,lport);
	                    }
	                    //c.tz=TimeZone.getTimeZone("GMT"); //set timezone to gmt
	            } catch (Exception ex) {
	                    System.err.println (ex);
	            }
	         Object res;
	         long[] lngs;
	         double[] dbls;
	         try {
	                res= c.k(Query);
	         } catch (KException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {}
	    	
	         
	         try {
	                res= c.k(Query);
	                if(res instanceof long[]){
	                    lngs=(long[])res;
	                    nrow=lngs.length;ncol=1;
	                    Object [][] tmp2=new Object[nrow][ncol]; //row x col
	                    for(int i=0;i<lngs.length;i++){
	                        tmp2[i][0]=(Double)(Long.valueOf(i)).doubleValue(); 
	                    }
	                    c.close();
	                    return tmp2;                    
	                  }
	                  else if(res instanceof double[]){
	                    dbls=(double[])res;
	                    nrow=dbls.length;ncol=1;
	                    Object [][] tmp2=new Object[nrow][ncol]; //row x col
	                    for(int i=0;i<dbls.length;i++){
	                    	tmp2[0][i]=(Double)dbls[i];
	                    }
	                    c.close();
	                    return tmp2;                    
	                  }
		    	    //tables
	                  else if(res instanceof c.Flip){
	                          model.setFlip((c.Flip) c.k(Query));
	                          nrow=model.getRowCount();
	                          ncol=model.getColumnCount();
	                          rowCount=nrow;
	                          //must use 0! if table is keyed
	                          tmp3 =new Object[nrow][ncol]; //row x col
	                            //  for(int i=0;i<ncol;i++){  //first the header
	                            //    tmp3[0][i]=model.getColumnName(i);
	                            //  }
	                          for(int i=0;i<nrow;i++){
		                          for(int j=0;j<ncol;j++){
			                          if(model.getValueAt(i,j) instanceof String)
			                        	  tmp3[i][j]=model.getValueAt(i,j);
			                          else if(model.getValueAt(i,j) instanceof Date)
			                        	  tmp3[i][j]=(Double)((Long) ((Date)(model.getValueAt(i,j))).getTime() ).doubleValue();
			                          else if(model.getValueAt(i,j) instanceof Long)
			                        	  tmp3[i][j]=(Double)((Long)(model.getValueAt(i,j))).doubleValue();
			                          else if(model.getValueAt(i,j) instanceof Second)
			                        	  tmp3[i][j]= (double)((Second)(model.getValueAt(i,j))).i ;
			                          else if(model.getValueAt(i,j) instanceof Minute) //convert to seconds
			                        	  tmp3[i][j]= 60*(double)((Minute)(model.getValueAt(i,j))).i ;
			                          else if(model.getValueAt(i,j) instanceof Double)
			                        	  tmp3[i][j]=model.getValueAt(i,j);
			                          else
			                        	  tmp3[i][j]="";   //works with null	                          
		                          }
	                          }
	                          c.close();
	                          return tmp3;
	                  } //end instance of flip
	         } catch (IOException e) {
				e.printStackTrace();
			} catch (KException e) {
				e.printStackTrace();
			} finally {}
	    }//if Query not empty   	
		return new Object[0][0];
    }
    
    public static JFreeChart buildChart() {
    	int rowCount=0;
    	int colCount=0;
    	Object [][] tmp3=new Object[1][1];
        JFreeChart chart;

        //Query="select from tab";Host=":5600";
        OHLCDataset ohlcds; 
        List<OHLCDataItem> dataItems = new ArrayList<OHLCDataItem>();
        //List<Double> dblx = new ArrayList<Double>();
    	//dblx.add(open);
    	//double[] tmpx = new double[dblx.size()]; int idx = 0; for (Double d : dblx) { tmpx[idx] = d; idx++; }
	    DefaultXYZDataset xyzds = new DefaultXYZDataset();
	    //DefaultXYDataset xyds = new DefaultXYDataset();
        if(Query!=null){ 
	        tmp3=getSeries(Host,Query);
	        rowCount=tmp3.length;
	        //rowCount=nrow; 
	        colCount=ncol;
        } else { rowCount=1; colCount=1; tmp3[0][0]=0.0; }
	    	XYSeries ser1_xy_data = new XYSeries("data");
	    	double[] dblx=new double[nrow];
	    	double[] dbly=new double[nrow];
	    	double[] dblz=new double[nrow];
	    	//use these 3 line to get timestamp since midnight 00:00 ...todo replace ...
	    	Calendar cal=Calendar.getInstance(TimeZone.getDefault());
	    	long curtime=cal.getTimeInMillis()/ 1000L;
	    	long midntsec=curtime-(cal.get(Calendar.HOUR_OF_DAY)*3600+cal.get(Calendar.MINUTE)*60+cal.get(Calendar.SECOND));
	        double lowestLow=0;
	        double highestHigh=1e100;
	    	for(int i=0;i<rowCount;i++){
	    		if(colCount>2){ //4 cols needed for bubble chart:sym,x,y,z(bubble size)
			    	if(chartType==5) {
			    		dblx[i]=(double)tmp3[i][1];
				    	dbly[i]=(double)tmp3[i][2]; //is yaxis range
				    	dblz[i]=(double)tmp3[i][3];
				    	if(dbly[i]<lowestLow) lowestLow=dbly[i];
				    	if(dbly[i]>highestHigh) highestHigh=dbly[i];
	    			}
			    } 
		        //ser1_xy_data.add(i,open);        	
	    		if(chartType<5) ser1_xy_data.add(colCount==1?i:(double)tmp3[i][0],colCount==1?(double)tmp3[i][0]:(double)tmp3[i][1]); //add (x,y) coordinates
	            if(colCount>=5){
	            	double open=  (double)tmp3[i][1];
		    		double high=  (double)tmp3[i][2];
		    		double low=   (double)tmp3[i][3];
		    		double close= (double)tmp3[i][4];
		    		double volume=colCount==5?0.0:(double)tmp3[i][5]; //if no vol set to 0
		    		double dat=	  (double)midntsec*1000 + 1000*(double)tmp3[i][0]; //add seconds to seconds since midnight
			        Date date = new Date((long)dat); //convert to date
			        OHLCDataItem item = new OHLCDataItem(date,open,high,low,close,volume);
		    	    dataItems.add(item);
		    	    if(i==0) {lowestLow=low;highestHigh=high;}
		    	    if(low<lowestLow) lowestLow=low;
			    	if(high>highestHigh) highestHigh=high;
	            }
	        }
            if(colCount>2){ //3 cols needed for bubble chart
		        double xyz[][] = { dblx , dbly , dblz };
		       // double xy[][] = { dblx , dbly };
		        xyzds.addSeries( "data" , xyz );
		       // xyds.addSeries( "" , xy );
            }
	        XYSeriesCollection my_data_series= new XYSeriesCollection();
	        my_data_series.addSeries(ser1_xy_data);
	        
	        Collections.reverse(dataItems);
	        OHLCDataItem[] data = dataItems.toArray(new OHLCDataItem[dataItems.size()]);
	        ohlcds=new DefaultOHLCDataset("data",data);
	        
            //Collections.reverse(dataItems);
            //OHLCDataItem[] data = dataItems.toArray(new OHLCDataItem[dataItems.size()]);
        
/*	
	    	String series1 = "First";
	    DefaultCategoryDataset my_data_series = new DefaultCategoryDataset();
        for(int i=0;i<rowCount;i++){
        	double open=(double)c.at(flip.y[1], i);
                    	
    	    my_data_series.addValue(open, series1, Integer.toString(i));
        }
*/	    
	    //NumberAxis axis = new NumberAxis();
        //NumberAxis          priceAxis     = new NumberAxis("Price");
        //priceAxis.setAutoRangeIncludesZero(false); //must not include zero
        
       // JFreeChart XYLineChart=ChartFactory.createXYLineChart("title","xlab","ylab",my_data_series,PlotOrientation.VERTICAL,true,true,false);
  //      JFreeChart XYLineChart=ChartFactory.createXYBarChart("title","xlab",false, "ylab",my_data_series,PlotOrientation.VERTICAL,true,true,false);
     //   JFreeChart XYLineChart=ChartFactory.createXYLineChart("title","xlab","ylab",my_data_series,PlotOrientation.VERTICAL,true,true,false);

       // String xlab="";
       // String ylab="";
        
        //JFreeChart XYLineChart=ChartFactory.createScatterPlot("title",xlab,ylab,my_data_series,PlotOrientation.VERTICAL,true,true,false);
    	chart=ChartFactory.createScatterPlot("","","",my_data_series,PlotOrientation.VERTICAL,true,true,false);
        if(chartType==0){
        	chart=ChartFactory.createScatterPlot("","","",my_data_series,PlotOrientation.VERTICAL,true,true,false);
        }
        else if(chartType==1){
        	chart=ChartFactory.createXYLineChart("","","",my_data_series,PlotOrientation.VERTICAL,true,true,false);
        }
        else if(chartType==2){
        	chart=ChartFactory.createXYBarChart("","",false,"",my_data_series,PlotOrientation.VERTICAL,true,true,false);
        }
        else if(chartType==3){ //select[5] time,lastprc from trades
        	chart=ChartFactory.createTimeSeriesChart("", "", "", my_data_series);
        }  //todo fix minute...
        else if(chartType==4){ //0!select Open:first lastprc,High:max lastprc,Low:min lastprc,Close:last lastprc,Volume:last volume-first volume by 30 xbar time.minute from trades where sym=`AAPL
        	chart=ChartFactory.createCandlestickChart("", "", "", ohlcds, false);
            //chart.getXYPlot().getRangeAxis().setRange(lowestLow*0.995, highestHigh*1.005);
            chart.getXYPlot().getRangeAxis().setRange(lowestLow-(highestHigh-lowestLow)*0.1, highestHigh+(highestHigh-lowestLow)*0.1);
        }
        else if(chartType==5){ //100#select from trades where sym=`AAPL
        	chart=ChartFactory.createBubbleChart("title", "time", "price", xyzds);
        	//chart=ChartFactory.createBubbleChart("", "", "", xyzds,PlotOrientation.VERTICAL,false,true,false);
        }
        else if(chartType==6){
  		  TreeMapNodeBuilder builder = new TreeMapNodeBuilder();
  		  TreeMapNode buildingRoot = builder.buildBranch("", null); //"branch"

  		  TreeMapNode box1 = builder.buildBranch("", buildingRoot); //"root"
  		  //1st value is size,2nd color in %
  		  TreeMapNode[] leaves=new TreeMapNode[nrow];
  		  
  		  //tm:([]sym:`AAPL`GOOG`AMZN`FB;a:12 23 12 34;b:0.7 0.4 1.6 -1.3)

  		  for(int i=0;i<rowCount;i++){
  			leaves[i]=builder.buildLeaf(tmp3[i][0].toString(), (double)tmp3[i][1], new ValuePercent((double)tmp3[i][2]), box1);
  		  }

  		  final JTreeMap jTreeMap = new JTreeMap(box1);
  		  jTreeMap.setFont(new Font(null, Font.BOLD, 10));
  		 
  		  jTreeMap.setPreferredSize(new Dimension(f.getWidth()-25,f.getHeight()-55-panel.getHeight()));//new Dimension(600, 400));
  		  jTreeMap.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
  		  
  		  final HSBTreeMapColorProvider provider = new HSBTreeMapColorProvider(jTreeMap,
  		  	HSBTreeMapColorProvider.ColorDistributionTypes.Linear, Color.GREEN, Color.RED);
  		  jTreeMap.setColorProvider(provider);
  		  
  		  	view.removeAll();
			view.add(jTreeMap);
        }
 /*       CategoryPlot plot = XYLineChart.getCategoryPlot();
        
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
	    final GradientPaint gp = new GradientPaint(
	            0.0f, 0.0f, Color.red, 
	            0.0f, 0.0f, Color.lightGray
	        );
	    renderer.setSeriesPaint(0, gp);
	       
*/
        
        chart.setTitle(""); //remove title
        if((chartType!=2)&&(chartType!=4)&&(chartType!=5)){
        XYLineAndShapeRenderer r = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();        
        r.setSeriesShape(0, ShapeUtilities.createDownTriangle(2));// createDiagonalCross(1, 1));// .createDiamond(2));
   //     XYTextAnnotation annotation = new XYTextAnnotation("test", 2.0, 2.0); 
   //     annotation.setFont(new Font("SansSerif", Font.PLAIN, 12)); 
        //annotation.setPaint(Color.BLUE);
        //annotation.setRotationAngle(Math.PI / 4.0);
   //     r.addAnnotation(annotation);
        
        r.setSeriesShapesVisible(0, true);
        } //else if(chartType==2){ //xybarchart
   //     	XYBarRenderer r = (XYBarRenderer) chart.getXYPlot().getRenderer();
            //r.setSeriesShape(0, ShapeUtilities.createDownTriangle(2));// createDiagonalCross(1, 1));// .createDiamond(2));
   //         XYTextAnnotation annotation = new XYTextAnnotation("test", 2.0, 2.0); 
   //         annotation.setFont(new Font("SansSerif", Font.PLAIN, 12)); 
            //annotation.setPaint(Color.BLUE);
            //annotation.setRotationAngle(Math.PI / 4.0);
   //         r.addAnnotation(annotation);
      //  } 
    	//else if(chartType==4){ //must create autoscale based on max high,min low
        	//CandlestickRenderer cdr = new CandlestickRenderer();
        	//NumberAxis          priceAxis     = new NumberAxis("");
        	//NumberAxis          axis     = new NumberAxis("");
        	//XYPlot              pricePlot     = new XYPlot(ohlcds, axis, priceAxis, cdr);	
        	//chart.getXYPlot().getRangeAxis().setRange(lowestLow*0.99, highestHigh*1.01);
        //} 
    	else if(chartType==5){ //http://stackoverflow.com/questions/19289323/how-to-set-tooltip-on-my-jfreechart
        	XYPlot xyplot = ( XYPlot )chart.getPlot( );  
        	XYItemRenderer r = xyplot.getRenderer( );
        	//r.getToolTipGenerator(arg0, arg1)
        	final String[] sym=new String[nrow];
    		for(int i=0;i<nrow;i++) sym[i]=tmp3[i][0].toString();
            XYToolTipGenerator xyToolTipGenerator = new XYToolTipGenerator()
            { //custom tooltip if symbol column exists
//        		String[] stk={"AAPL","FB","AMZN","INTC","NFLX"};      		
        		public String generateToolTip(XYDataset dataset, int series, int item) {
                    Number x1 = dataset.getX(series, item);
                    Number y1 = dataset.getY(series, item);
                    StringBuilder stringBuilder = new StringBuilder();
                    //stringBuilder.append(String.format("<html><p style='color:#0000ff;'>Serie: '%s'</p>", dataset.getSeriesKey(series)));
                    //stringBuilder.append(String.format("X:'%d'<br/>", x1.intValue()));
                    //stringBuilder.append(String.format("Y:'%d'", y1.intValue()));
                    //stringBuilder.append("</html>");
                    stringBuilder.append(x1.intValue()+","+y1.intValue()+","+sym[item]);
                    return stringBuilder.toString();
                }	
            };
            r.setBaseToolTipGenerator(xyToolTipGenerator);	
        }
        return chart;
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                DynChart dchart = new DynChart();
            }
        });
    }
}