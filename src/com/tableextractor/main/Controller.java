package com.tableextractor.main;

import java.util.ArrayList;

public class Controller {
	public static void main(String[] args){
		String dbName= "northwind";
		String tableName= "employees";
		
		//Insert into table
		
		/*ArrayList<Object []> excelData= new ArrayList<Object []>();
		ExcelToDb ex= new ExcelToDb();
		excelData= ex.getExcelData(dbName, tableName);
		ex.insertData(excelData,dbName,tableName);*/
		
		//Insert into sheet
		
		/*DbToExcel db= new DbToExcel();
		Object [] data= db.getTableData(dbName, tableName);
		ArrayList<Object> metaData= (ArrayList<Object>)data[0];
		ArrayList<ArrayList<Object>> tableData= (ArrayList<ArrayList<Object>>) data[1];
		db.writeTableToExcel(metaData, tableData, dbName, tableName);*/
		
	}
}
