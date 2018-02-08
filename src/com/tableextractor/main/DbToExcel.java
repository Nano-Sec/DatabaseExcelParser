package com.tableextractor.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Represents the transfer of a table from a database to the specified excel sheet
 * @author Abhinav Upadhyay
 *
 */
public class DbToExcel {
	private static final Logger log= Logger.getLogger(DbToExcel.class);
	private static final Logger filelog= Logger.getLogger(DbToExcel.class);
	/**
	 * Retrieves table data from the database
	 * @param dbname Name of the database in the connection
	 * @param tableName Name of the table in the database
	 * @return Object with encapsulated metadata and tabledata
	 */
	public Object [] getTableData(String dbname, String tableName){
		QueryRunner run = new QueryRunner();
		Connection conn= null;
		Object [] data= null;
		try{
			conn= Dbcon.getCon(dbname);
			log.info("Connection made to database " + dbname);
			data = run.query(conn,"select * from "+tableName, rsh);
		}
		catch(SQLException e){
			log.error(e.getMessage(), e);
		}
		finally{
			DbUtils.closeQuietly(conn);
			log.info("Connection to database closed");
		}
		return data;
	}
	
	ResultSetHandler<Object []> rsh= new ResultSetHandler<Object []> (){
		public Object[] handle(ResultSet rs) throws SQLException{
			if (!rs.isBeforeFirst()) {
				log.error("ResultSet empty");
	            return null;
	        }
			ArrayList<Object> metaData=getMetaData(rs);
			ArrayList<List<Object>> data= getData(rs);
			return new Object [] {metaData, data};
		}
	};
	
	/**
	 * Retrieves the column names of the specified table 
	 * @param rs Resultset object received by the handler
	 * @return ArrayList of column names
	 * @throws SQLException
	 */
	public ArrayList<Object> getMetaData(ResultSet rs) throws SQLException{
		ResultSetMetaData meta= rs.getMetaData();
		ArrayList<Object> metaData= new ArrayList<Object>();
		int cols= meta.getColumnCount();
		for(int i=0; i< cols; i++)
			metaData.add(i, meta.getColumnName(i+1));
		return metaData;
	}
	
	/**
	 * Retrieves the table data of the specified table	
	 * @param rs Resultset object received by the handler
	 * @return Arraylist of arraylist of objects where each object represents a table cell
	 * @throws SQLException
	 */
	public ArrayList<List<Object>> getData(ResultSet rs) throws SQLException{
		ArrayList<List<Object>> data= new ArrayList<List<Object>>();
		int count= rs.getMetaData().getColumnCount();
		while(rs.next()){
			List<Object> row= new ArrayList<Object>();
			for(int i=0;i<count;i++)
				row.add(rs.getObject(i+1));
			data.add(row);
		}
		if(data.size()>0)
			log.info("Data retrieved from table");
		else
			filelog.debug("table dataset empty");
		return data;
	}
	
	/**
	 * Parses the entire table data along with the metadata into the specified excel sheet
	 * @param metaData ArrayList of column names as objects
	 * @param tableData Arraylist of arralylist of objects as table data
	 * @param wbName Name of the excel workbook
	 * @param sheetName Name of the sheet to create
	 */
	public void writeTableToExcel(ArrayList<Object> metaData, ArrayList<ArrayList<Object>> tableData, String wbName, String sheetName){
		try{
			FileOutputStream out= null;
			XSSFWorkbook workbook= new XSSFWorkbook();
			XSSFSheet spreadsheet = workbook.createSheet(sheetName);
			XSSFRow row= spreadsheet.createRow(0);
	
			for(Object heading: metaData)
				row.createCell(metaData.indexOf(heading)).setCellValue((String)heading);
	
			for(ArrayList<Object> tableRow: tableData){
				row=spreadsheet.createRow(tableData.indexOf(tableRow)+1);
				for(Object tableCell: tableRow){
					if(tableCell instanceof Integer)
						row.createCell(tableRow.indexOf(tableCell)).setCellValue((Integer)tableCell);
					else if(tableCell instanceof Long)
						row.createCell(tableRow.indexOf(tableCell)).setCellValue((long)tableCell);
					else if(tableCell instanceof Float)
						row.createCell(tableRow.indexOf(tableCell)).setCellValue((float)tableCell);
					else if(tableCell instanceof Double)
						row.createCell(tableRow.indexOf(tableCell)).setCellValue((double)tableCell);
					else if(tableCell instanceof Boolean)
						row.createCell(tableRow.indexOf(tableCell)).setCellValue((boolean)tableCell);
					else if(tableCell instanceof String)
						row.createCell(tableRow.indexOf(tableCell)).setCellValue((String)tableCell);
					else if(tableCell == null){
						row.createCell(tableRow.indexOf(tableCell)).setCellValue("");
					}
					else{
						row.createCell(tableRow.indexOf(tableCell)).setCellValue(tableCell.toString());
						filelog.debug("unsupported data type");
					}
					spreadsheet.autoSizeColumn(tableRow.indexOf(tableCell));
				}
			}
			try{
				out = new FileOutputStream(new File(wbName+".xlsx"));
				workbook.write(out);
			}
			finally{
				out.flush();
				out.close();
			}
		}
		catch (IOException e){
			log.error(e.getMessage(), e);
		}
	}
}
