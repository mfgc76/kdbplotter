# kdbplotter
easily plot charts from data in kdb

This stand alone java app connects to a kdb database and uses the resulting
list or table to create charts
- point, line, bar, timeseries, candlestick and bubblecharts (using the jfreecharts library)
- and treemap charts (using jtreemap)
- autorefresh timer in seconds can be set, to remove untick
- point,line,bar charts expect a list or a table with one or 2 columns
- timeseries requires a 2 column table (x:time,y:data)
- candlestick requires a 5 or 6 column table (x:time,y:open,high,low,close,volume), with volume being optional
- bubblechart requires a 4 column table ordered as: 1:symbol,2:x-value,3:y-value,4:bubble size
- treemap requires a 3 column table ordered as: 1:symbol,2:rect size,3:color

It can be useful especially in finance

Example

-for a 5min candlestick chart using 1min ohlcv data: 
0!select Open:first lastprc,High:max lastprc,Low:min lastprc,Close:last lastprc,Volume:last volume-first volume by 5 xbar time.minute from trades where sym=`AAPL ,time>=09:30,time<16:00

![<oocalc image>](https://github.com/mfgc76/kdbplotter/blob/master/img/kdbplotter1.png)

- To run double click the excutable or use java -jar kdbplotter.jar. The large filesize is due to the included jfreechart library(over 1mb)
- To build import project into Eclipse, jfreecharts and jtreemap libraries are included
- Built with Java7, tested with [kdb+](https://kx.com/) 3.2, [jfreecharts](http://www.jfree.org/jfreechart/) 1.0.19, [jtreemap](http://jtreemap.sourceforge.net/)
