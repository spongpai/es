package com.eventshop.eventshoplinux.DAO.datasource;

import com.eventshop.eventshoplinux.DAO.BaseDAO;
import com.eventshop.eventshoplinux.model.DataSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.eventshop.eventshoplinux.constant.Constant.*;

/**
 * Created by abhisekmohanty on 4/8/15.
 */
public class DataSourceDao extends BaseDAO{

    public int registerDatasource (DataSource dataSource) {
        PreparedStatement ps = null;
        ResultSet rs= null;
        try {
            int inserted = 0;
            int key = 0;
            if(con.isClosed())
                con = this.connection();
            ps = con.prepareStatement(INSERT_DATASOURCE_QRY, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, dataSource.getName());
            ps.setString(2, dataSource.getTheme());
            ps.setString(3, dataSource.getUrl());
            ps.setString(4, dataSource.getFormat());
            ps.setInt(5, dataSource.getUser_Id());
            ps.setString(6, dataSource.getSyntax());
            inserted = ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if ( rs.next() ) {
                // Retrieve the auto generated key(s).
                key = rs.getInt(1);
            }

            if (key != 0) {
                ps = con.prepareStatement(INSERT_DATASOURCE_RESOLUTION_QRY);
                ps.setInt(1, key);
                ps.setLong(2, dataSource.getTime_Window());
                ps.setDouble(3, dataSource.getLatitude_Unit());
                ps.setDouble(4, dataSource.getLongitude_Unit());
                ps.setString(5, dataSource.getBoundingbox());
                ps.setLong(6, dataSource.getSync_Time());
                inserted = ps.executeUpdate();
            }

            if (inserted != 0) {
                ps = con.prepareStatement(INSERT_WRAPPER_QRY);
                ps.setString(1, dataSource.getWrapper_Name());
                ps.setString(2, dataSource.getWrapper_Key_Value());
                ps.setString(3, dataSource.getBag_Of_Words());
                ps.setInt(4, key);
                inserted = ps.executeUpdate();
            }
            if (inserted != 0 ) {
                return key;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) { /* ignored */ }
            try { if (ps != null) ps.close(); } catch (Exception e) { /* ignored */ }
            try { if (con != null) con.close(); } catch (Exception e) { /* ignored */ }
        }
        return 0;
    }

    public String deleteDatasource (int id) {
        int deleted = 0;
        PreparedStatement ps = null;
        try {
            if(con.isClosed())
                con = this.connection();
            ps = con.prepareStatement(DELETE_WRAPPER_QUERY);
            ps.setInt(1, id);
            deleted = ps.executeUpdate();

            if(deleted != 0) {
                ps = con.prepareStatement(DELETE_DS_RESOLUTION_QUERY);
                ps.setInt(1, id);
                deleted = ps.executeUpdate();
            }

            if(deleted != 0) {
                ps = con.prepareStatement(DELETE_DS_MASTER_QUERY);
                ps.setInt(1, id);
                deleted = ps.executeUpdate();
            }
            if(deleted != 0)
                return "DataSource deleted succefully";

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            try { if (ps != null) ps.close(); } catch (Exception e) { /* ignored */ }
            try { if (con != null) con.close(); } catch (Exception e) { /* ignored */ }
        }

        return "Exception in deleting Datasource";
    }

    public boolean getDsStatus(int dsId) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if(con.isClosed())
                con = this.connection();
            ps = con.prepareStatement(DS_STAT_QRY);
            ps.setInt(1, dsId);
            rs = ps.executeQuery();
            if ( rs.next() ) {
                String stat = rs.getString(1);
                if (stat.equalsIgnoreCase("1"))
                    return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) { /* ignored */ }
            try { if (ps != null) ps.close(); } catch (Exception e) { /* ignored */ }
            try { if (con != null) con.close(); } catch (Exception e) { /* ignored */ }
        }
        return false;
    }

    public boolean checkLinkedQuery(int id) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String ds = "ds" + id;
            if(con.isClosed())
                con = this.connection();
            ps = con.prepareStatement(GET_LINKED_DS);
            rs = ps.executeQuery();
            while (rs.next()) {

                if (!rs.getString("linked_ds").isEmpty() && rs.getString("linked_ds").contains(ds)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) { /* ignored */ }
            try { if (ps != null) ps.close(); } catch (Exception e) { /* ignored */ }
            try { if (con != null) con.close(); } catch (Exception e) { /* ignored */ }
        }
        return false;
    }

}
