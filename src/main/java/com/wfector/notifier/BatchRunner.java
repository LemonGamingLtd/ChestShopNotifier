package com.wfector.notifier;

import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BatchRunner extends WrappedRunnable {
    private ChestShopNotifier plugin;

    public BatchRunner(ChestShopNotifier main) {
        this.plugin = main;
    }

    @Override
    public void run() {
        plugin.debug("Uploading a batch...");

        if (!plugin.isPluginEnabled()) return;

        if (plugin.getBatch().size() > 0) {

            try (Connection conn = plugin.getConnection()){
                conn.setAutoCommit(false);
                String qstr = "INSERT INTO csnUUID (`ShopOwnerId`, `CustomerId`, `CustomerName`, `ItemId`, `Mode`, `Amount`, `Time`, `Quantity`, `Unread`) VALUES (?,?,?,?,?,?,?,?,?)";

                PreparedStatement statement = conn.prepareStatement(qstr);

                int i = 0;
                HistoryEntry entry;
                while ((entry = plugin.getBatch().poll()) != null) {
                    i++;

                    statement.setString(1, entry.getShopOwnerId().toString());
                    statement.setString(2, entry.getCustomerId().toString());
                    statement.setString(3, entry.getCustomerName());
                    statement.setString(4, entry.getItemId());
                    statement.setInt(5, entry.getType().ordinal() + 1);
                    statement.setDouble(6, entry.getAmountPaid());
                    statement.setInt(7, entry.getTime());
                    statement.setInt(8, entry.getQuantity());
                    statement.setInt(9, entry.isUnread() ? 0 : 1);

                    statement.addBatch();
                    if (i % 1000 == 0) {
                        statement.executeBatch(); // Execute every 1000 items.
                        conn.commit();
                    }
                }

                // Check iteration count to see if we have uncommitted data
                if (i % 1000 != 0) {
                    statement.executeBatch();
                    conn.commit();
                }

                plugin.debug("Update: " + qstr);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            plugin.debug("Batch completed.");
        }
    }
}
