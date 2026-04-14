package me.ogsammaenr.skyblockCore.data;

import com.zaxxer.hikari.HikariDataSource;
import me.ogsammaenr.skyblockApi.math.BlockCoordinate;
import me.ogsammaenr.skyblockApi.math.Location;
import me.ogsammaenr.skyblockApi.model.Island;
import me.ogsammaenr.skyblockApi.model.IslandID;
import me.ogsammaenr.skyblockApi.repository.IslandRepository;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SqliteIslandRepository implements IslandRepository {

    private final HikariDataSource dataSource;
    private final ExecutorService dbExecutor = Executors.newFixedThreadPool(4); // SQL işlemleri için özel thread pool

    public SqliteIslandRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        createTables();
    }

    private void createTables() {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "CREATE TABLE IF NOT EXISTS islands (" +
                                 "id VARCHAR(36) PRIMARY KEY, owner_id VARCHAR(36), " +
                                 "center_x INT, center_y INT, center_z INT, " +
                                 "spawn_x DOUBLE, spawn_y DOUBLE, spawn_z DOUBLE, spawn_yaw FLOAT, spawn_pitch FLOAT);"
                 )) {
                ps.executeUpdate();
                // Not: island_members tablosu da burada oluşturulmalı (island_id, player_id, role_weight)
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, dbExecutor);
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<Island>> loadIsland(@NotNull IslandID id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM islands WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id.toString());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    UUID ownerId = UUID.fromString(rs.getString("owner_id"));
                    BlockCoordinate center = new BlockCoordinate(rs.getInt("center_x"), rs.getInt("center_y"), rs.getInt("center_z"));
                    Island island = new Island(id, ownerId, center);

                    Location spawn = new Location(rs.getDouble("spawn_x"), rs.getDouble("spawn_y"), rs.getDouble("spawn_z"),
                            rs.getFloat("spawn_yaw"), rs.getFloat("spawn_pitch"));
                    island.setSpawnPoint(spawn);
                    // Not: Members tablosuna da JOIN veya ayrı bir sorgu atılıp island.addMember() yapılmalı.
                    return Optional.of(island);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Optional.empty();
        }, dbExecutor);
    }

    @Override
    public void saveIsland(@NotNull Island island) {
        CompletableFuture.runAsync(() -> {
            String sql =
                    "INSERT INTO islands (id, owner_id, center_x, center_y, center_z, spawn_x, spawn_y, spawn_z, spawn_yaw, spawn_pitch) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT(id) DO UPDATE SET spawn_x=excluded.spawn_x, spawn_y=excluded.spawn_y, spawn_z=excluded.spawn_z, " +
                    "spawn_yaw=excluded.spawn_yaw, spawn_pitch=excluded.spawn_pitch;"; // SQLite/PostgreSQL Upsert mantığı

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, island.getIslandID().toString());
                // ... (Diğer parametreler set edilecek)
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, dbExecutor);
    }

    @Override
    public void deleteIsland(@NotNull IslandID id) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM islands WHERE id = ?")) {
                ps.setString(1, id.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, dbExecutor);
    }

    // --- Arayüzdeki diğer metotlar (getIslandIdByOwner vb.) benzer şekilde PreparedStatement ile eklenecek ---

    @Override
    public @NotNull CompletableFuture<Optional<IslandID>> getIslandIdByOwner(@NotNull UUID ownerId) {
        return CompletableFuture.supplyAsync(() -> Optional.empty()); // TODO: SELECT id FROM islands WHERE owner_id = ?
    }

    @Override
    public @NotNull CompletableFuture<List<Island>> getIslandsByMember(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>()); // TODO: INNER JOIN ile yazılacak
    }

    @Override
    public @NotNull CompletableFuture<List<IslandID>> getAllIslandIds() {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>()); // TODO: SELECT id FROM islands
    }

    @Override
    public void saveAll(@NotNull Collection<Island> islands) {
        CompletableFuture.runAsync(() -> {
            // Batch Insert mekanizması (ps.addBatch(), ps.executeBatch()) kullanılacak.
        }, dbExecutor);
    }
}