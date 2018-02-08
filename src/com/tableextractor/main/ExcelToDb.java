package com.tableextractor.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Represents the insertion of table data from the specified excel sheet
 * @author Abhinav Upadhyay
 *
 */
public class ExcelToDb {
	private static final Logger log= Logger.getLogger(ExcelToDb.class);
	private static final Logger filelog= Logger.getLogger(DbToExcel.class);
	/**
	 * Retrieves the table data from the specified excel sheet
	 * @param wbName Name of the excel workbook
	 * @param sheetName Name of the excel sheet in the workbook
	 * @return Arraylist of string arrays as table data
	 */
	public ArrayList<Object []> getExcelData(String wbName, String sheetName){
		ArrayList<Object []> tableData= null;
		try{
			tableData=  new ArrayList<Object []>();
			File file = new File(wbName+".xlsx");
			InputStream ExcelFileToRead= new FileInputStream(file);
			XSSFWorkbook workbook= new XSSFWorkbook(ExcelFileToRead);
			XSSFSheet spreadsheet= workbook.getSheet(sheetName);
			XSSFRow row;
			XSSFCell cell;
			Iterator<Row> rows= spreadsheet.rowIterator();
			row= (XSSFRow) rows.next();
			while(rows.hasNext()){
				row= (XSSFRow) rows.next();
				Iterator<Cell> cells = row.cellIterator();
				int count= row.getLastCellNum();
				Object [] tableRow= new Object [count];
				while(cells.hasNext()){
					cell=(XSSFCell) cells.next();
					switch(cell.getCellType()){
					case Cell.CELL_TYPE_BLANK:
						tableRow[cell.getColumnIndex()]= "";
						break;
					case Cell.CELL_TYPE_NUMERIC:
						cell.setCellType(Cell.CELL_TYPE_STRING);
						tableRow[cell.getColumnIndex()]= cell.getStringCellValue();
						break;
					case Cell.CELL_TYPE_BOOLEAN:
						tableRow[cell.getColumnIndex()]= String.valueOf(cell.getBooleanCellValue());
						break;
					case Cell.CELL_TYPE_STRING:
						tableRow[cell.getColumnIndex()]= cell.getStringCellValue();
						break;
					default:
						tableRow[cell.getColumnIndex()]= null;
						break;
					}
				}
				tableData.add(tableRow);
			}
			if(tableData.size()>0)
				log.info("Data retrieved from excel");
			else
				filelog.debug("tableData size is 0");
		}
		catch(IOException e){
			log.error(e.getMessage(), e);
		}
		return tableData;
	}
	
	ResultSetHandler<String []> rsh= new ResultSetHandler<String []> (){
		public String[] handle(ResultSet rs) throws SQLException{
			if (!rs.next()) {
				log.error("ResultSet empty");
	            return null;
	        }
			ResultSetMetaData meta= rs.getMetaData();
			String [] types= new String [meta.getColumnCount()];
			for(int i=0; i< types.length; i++){
				types[i]= meta.getColumnTypeName(i+1);
			}
			return types;
		}
	};
	/**
	 * Inserts the parsed table data into the specified database and table
	 * @param data Arraylist of string arrays containing the table data
	 * @param dbName Name of the database to be used
	 * @param tableName Name of the table to be updated
	 */
	public void insertData(ArrayList<Object []> data, String dbName, String tableName){
		Connection conn= null;
		QueryRunner run= new QueryRunner();
		ArrayList<Object[]> queryParams= new ArrayList<Object[]>();
		try{
			conn= Dbcon.getCon(dbName);
			log.info("Connection to database " + dbName+ " established");
			String [] types= run.query(conn, "select * from "+tableName, rsh);
			for(Object[] row: data){
				Object[] dataRow= new Object[row.length];
				for(int i=0; i< row.length; i++){
					dataRow[i]= convert(types[i], row[i]);
				}
				queryParams.add(dataRow);
			}
			Object[][] params= new Object[queryParams.size()][queryParams.get(0).length];
			for(int i=0; i<queryParams.size(); i++)
				params[i]= queryParams.get(i);
			String sql= "insert into "+tableName+" values(?";
			for(int i=1;i<queryParams.get(0).length;i++)
				sql+=",?";
			sql+=")";
			int queryResult[] = run.batch(conn, sql, params);
			for(int query: queryResult)
				if(query<=0)
					filelog.debug("One or more inserts failed");
		}
		catch(SQLException e){
			log.error(e.getMessage(), e);
		}
		catch(ParseException e){
			log.error("Problem parsing the date type", e);
		}
		finally {
            DbUtils.closeQuietly(conn);
            log.info("Connection to database closed");
        }
	}
	/**
	 * Converts the table cell strings to the corresponding table column data types and encapsulates them in an object
	 * @param type Column data type as string 
	 * @param itemToConvert The table cell string to be converted 
	 * @return Converted object
	 * @throws ParseException
	 */
	public Object convert(String type, Object itemToConvert) throws ParseException{
		Object item= new Object();
		if(itemToConvert == null)
			return item=null;	
		switch(type){
		case "NUMERIC":
			item= new BigDecimal((String)itemToConvert);
			break;
		case "DECIMAL":
			item= new BigDecimal((String)itemToConvert);
			break;
		case "BIT":
			item= (boolean)itemToConvert;
			break;
		case "TINYINT":
			item= (int)itemToConvert;
			break;
		case "SMALLINT":
			item= (int)itemToConvert;
			break;
		case "INTEGER":
			item= (int)itemToConvert;
			break;
		case "BIGINT":
			item= (long)itemToConvert;
			break;
		case "REAL":
			item= (float)itemToConvert;
			break;
		case "FLOAT":
			item= Double.valueOf((String)itemToConvert);
			break;
		case "DOUBLEPRECISION":
			item= Double.valueOf((String)itemToConvert);
			break;
		case "DATE":
			item= (Date)itemToConvert;
			break;
		case "TIME":
			item= Time.valueOf(itemToConvert.toString());
			break;
		case "TIMESTAMP":
			item=(Timestamp)itemToConvert;
			break;
		default:
			item= itemToConvert.toString();
			break;
		}
		return item;
	}
}
