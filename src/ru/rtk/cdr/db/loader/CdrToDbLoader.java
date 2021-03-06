package ru.rtk.cdr.db.loader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import oracle.jdbc.pool.OracleDataSource;
import ru.rtk.file.handler.GlobalArgs;

public class CdrToDbLoader {
    Connection conn = null;
    String md5sum = null;
    PreparedStatement pstmt = null;
    Statement stmt = null;

    public CdrToDbLoader(){
        checkdbconn();
    }

    void checkdbconn()
    {
        String jdbcUrl = "jdbc:oracle:thin:" +
                GlobalArgs.dblogin + "/" +
                GlobalArgs.dbpassword + "@" +
                GlobalArgs.rdbmsip + ":1521:" +
                GlobalArgs.dbsid;
        Statement stmt = null;
        ResultSet rset = null;
        try
        {
            OracleDataSource ds;
            ds = new OracleDataSource();
            ds.setURL(jdbcUrl);
            conn = ds.getConnection();
            conn.setAutoCommit (false);
            stmt = conn.createStatement ();
            rset = stmt.executeQuery ("select 'Hello World' from dual");
            int c = 0;
            while (rset.next ())
            {
                c++;
            }
            if(c > 0)
                System.out.println (GlobalArgs.curdate() + "-->соединение с базой данных установлено");
            rset.close();
            stmt.close();
            rset = null;
            stmt = null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (rset != null)
            {
                try { rset.close(); } catch (Exception e) {e.printStackTrace();}
            }
            if (stmt != null)
            {
                try { stmt.close(); } catch (Exception e) {e.printStackTrace();}
            }
            rset = null;
            stmt = null;
        }
    }

    void writeToLoadFileLog(String line)
    {
        try
        {
            conn.setAutoCommit (true);

            pstmt = conn.prepareStatement(
                    "INSERT INTO LOADFILELOGS (FILENAME,LDSTART,MD5SUM,NOTE) " +
                            "VALUES(?,?,?,?)");
            pstmt.setString (1, GlobalArgs.filename);
            pstmt.setTimestamp (2, java.sql.Timestamp.valueOf(GlobalArgs.curdate()));
            pstmt.setString (3, md5sum);
            pstmt.setString (4, line);
            pstmt.execute();
            pstmt.close();

            conn.setAutoCommit (false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    boolean checkfileinbase()
    {
        boolean res = false;
        ResultSet rset = null;
        try
        {
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
            String SQL = "SELECT * FROM loadfilelogs WHERE (md5sum = '" +
                    md5sum + "') AND (note = 'данные из файла загружены успешно')";
            rset = stmt.executeQuery(SQL);

            if(rset.isBeforeFirst())
            {
                res = true;
            }
            else
            {
                res = false;
            }

            rset.close();
            rset = null;
            stmt.close();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return res;
    }

    int getCdrFileIdForLoad()
    {
        ResultSet rset = null;
        int id = 0;
        try
        {
            stmt = conn.createStatement();
            String req = "SELECT * FROM loadfilelogs WHERE (md5sum = '" +
                    md5sum +
                    "') AND (note = 'начало загрузки данных из файла') ORDER BY id DESC";
            rset = stmt.executeQuery(req);
            while (rset.next())
            {
                id = rset.getInt("id");
            }
            rset.close();
            rset = null;
            stmt.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return id;
    }

    boolean insert_data_to_table()
    {
        int file_id = getCdrFileIdForLoad();
        try
        {
            if(GlobalArgs.typeATS.equals("Si2000")){ //если это файлы Si2000
                //загружаем файлы Si2000
                String SQL = "INSERT INTO SI2000 (anum,bnum,chargstart,dur,incrt,outrt,rectype,loadfilelog_id) " +
                        "VALUES(?,?,?,?,?,?,?,?)";
                pstmt = conn.prepareStatement(SQL);
                conn.setAutoCommit (false);

                System.out.print(GlobalArgs.curdate() + "-->начинаю передачу данных ");
                int point = 0;
                for (int i = 0; i < GlobalArgs.si2Kparser.getANum().size(); i++) {
                    pstmt.setString(1, GlobalArgs.si2Kparser.getANum().get(i));
                    pstmt.setString(2, GlobalArgs.si2Kparser.getBNum().get(i));
                    pstmt.setTimestamp(3, java.sql.Timestamp.valueOf(GlobalArgs.si2Kparser.getDat().get(i)));
                    pstmt.setInt(4, GlobalArgs.si2Kparser.getDur().get(i));
                    pstmt.setString(5, GlobalArgs.si2Kparser.getIncRt().get(i));
                    pstmt.setString(6, GlobalArgs.si2Kparser.getOutRt().get(i));
                    pstmt.setString(7, GlobalArgs.si2Kparser.getRecType().get(i));
                    pstmt.setInt(8, file_id);
                    pstmt.addBatch();
                    point++;
                    if(point==100){
                        System.out.print(".");
                        point = 0;
                    }
                }
                System.out.println("\n" + GlobalArgs.curdate() + "-->передача данных завершена");
                pstmt.executeBatch();
                pstmt.close();
                conn.setAutoCommit (true);
                //конец загрузки файлов Si2000
            }
            if(GlobalArgs.typeATS.equals("AXE10")){ //если это файлы АХЕ-10
                //загружаем файлы АХЕ-10
                String SQL = "INSERT INTO AXE10 (anum,bnum,chargstart,dur,incrt,outrt,rectype,loadfilelog_id) " +
                        "VALUES(?,?,?,?,?,?,?,?)";
                pstmt = conn.prepareStatement(SQL);
                conn.setAutoCommit (false);

                System.out.print(GlobalArgs.curdate() + "-->начинаю передачу данных ");
                int point = 0;
                for (int i = 0; i < GlobalArgs.hTTFile.getANum().size(); i++) {
                    pstmt.setString(1, GlobalArgs.hTTFile.getANum().get(i));
                    pstmt.setString(2, GlobalArgs.hTTFile.getBNum().get(i));
                    pstmt.setTimestamp(3, java.sql.Timestamp.valueOf(GlobalArgs.hTTFile.getDat().get(i)));
                    pstmt.setInt(4, GlobalArgs.hTTFile.getDur().get(i));
                    pstmt.setString(5, GlobalArgs.hTTFile.getIncRt().get(i));
                    pstmt.setString(6, GlobalArgs.hTTFile.getOutRt().get(i));
                    pstmt.setString(7, GlobalArgs.hTTFile.getRecType().get(i));
                    pstmt.setInt(8, file_id);
                    pstmt.addBatch();
                    point++;
                    if(point==100){
                        System.out.print(".");
                        point = 0;
                    }
                }
                System.out.println("\n" + GlobalArgs.curdate() + "-->передача данных завершена");
                pstmt.executeBatch();
                pstmt.close();
                conn.setAutoCommit (true);
                //конец загрузки файлов АХЕ-10
            }
            //----------------------------------------------------------------------------------------------
            if(GlobalArgs.typeATS.equals("NEAX61")){ //если это файлы NEAX61
                String SQL = "INSERT INTO NEAX61 (anum,bnum,chargstart,dur,incrt,outrt,rectype,loadfilelog_id) " +
                        "VALUES(?,?,?,?,?,?,?,?)";
                pstmt = conn.prepareStatement(SQL);
                conn.setAutoCommit (false);

                System.out.print(GlobalArgs.curdate() + "-->начинаю передачу данных ");
                int point = 0;
                for (int i = 0; i < GlobalArgs.hCDRFile.getANum().size(); i++) {
                    pstmt.setString(1, GlobalArgs.hCDRFile.getANum().get(i));
                    pstmt.setString(2, GlobalArgs.hCDRFile.getBNum().get(i));
                    pstmt.setTimestamp(3, java.sql.Timestamp.valueOf(GlobalArgs.hCDRFile.getDat().get(i)));
                    pstmt.setInt(4, GlobalArgs.hCDRFile.getDur().get(i));
                    pstmt.setString(5, GlobalArgs.hCDRFile.getIncRt().get(i));
                    pstmt.setString(6, GlobalArgs.hCDRFile.getOutRt().get(i));
                    pstmt.setString(7, GlobalArgs.hCDRFile.getRecType().get(i));
                    pstmt.setInt(8, file_id);
                    pstmt.addBatch();
                    point++;
                    if(point==100){
                        System.out.print(".");
                        point = 0;
                    }
                }
                System.out.println("\n" + GlobalArgs.curdate() + "-->передача данных завершена");
                pstmt.executeBatch();
                pstmt.close();
                conn.setAutoCommit (true);
                //конец загрузки файлов NEAX61
            }

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }

    }

    public boolean LoadCdrToDb()
    {
        boolean result = false;
        md5sum = GlobalArgs.md5sum();
        writeToLoadFileLog("попытка загрузить данные из файла");
        long begintime = System.currentTimeMillis();
        if(checkfileinbase() != true)
        {
            writeToLoadFileLog("начало загрузки данных из файла");
            if(insert_data_to_table())
            {
                writeToLoadFileLog("конец загрузки данных из файла");
                writeToLoadFileLog("данные из файла загружены успешно");
                GlobalArgs.cause = "";
                result = true;
            }
            else
            {
                writeToLoadFileLog("данные из файла загрузить не удалось");
                GlobalArgs.cause = ", неизвестная причина";
            }
        }
        else
        {
            System.out.println(GlobalArgs.curdate() + "-->данные из файла уже загружены: " + GlobalArgs.filename);
            writeToLoadFileLog("данные из файла уже загружены");
            GlobalArgs.cause = ", данные из файла уже загружены";
        }
        long endtime = System.currentTimeMillis() - begintime ;
        System.out.println(GlobalArgs.curdate() + "-->время выполнения = " + endtime + " миллисекунд");

        return result;
    }

    public void closeconn(){
        try
        {
            pstmt = null;
            stmt = null;
            conn.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
