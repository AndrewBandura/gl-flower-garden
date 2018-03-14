package com.flowergarden.dao.impl;

import com.flowergarden.dao.FetchMode;
import com.flowergarden.dao.FlowerDao;
import com.flowergarden.dto.BouquetDto;
import com.flowergarden.dto.DtoMapper;
import com.flowergarden.dto.FlowerDto;
import com.flowergarden.model.bouquet.Bouquet;
import com.flowergarden.model.flowers.Chamomile;
import com.flowergarden.model.flowers.GeneralFlower;
import com.flowergarden.model.flowers.Rose;

import com.flowergarden.dto.BouquetDto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Andrew Bandura
 */
public class FlowerDaoImpl implements FlowerDao {

    private Connection connection;

    public FlowerDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public int add(GeneralFlower flower) {

        int newId = 0;

        try {
            PreparedStatement stmt = connection.prepareStatement(SQL_ADD);
            stmt.setObject(1, flower.getName());
            stmt.setObject(2, flower.getLenght());
            stmt.setObject(3, flower.getFreshness().getFreshness());
            stmt.setObject(4, flower.getPrice());
            Bouquet bouquet = flower.getBouquet();

            if(!(bouquet==null)){
                stmt.setObject(7, bouquet.getId());
            }

            if (flower instanceof Rose) {
                stmt.setObject(5, null);
                stmt.setObject(6, ((Rose) flower).isSpike());
            } else if (flower instanceof Chamomile) {
                stmt.setObject(5, ((Chamomile) flower).getPetals());
                stmt.setObject(6, null);
            }

            stmt.execute();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    newId = generatedKeys.getInt(1);
                    flower.setId(newId);
                } else {
                    throw new SQLException("Adding flower failed, no ID obtained.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return newId;
    }

    @Override
    public Optional<GeneralFlower> read(int id, FetchMode fetchMode) {

        Optional<GeneralFlower> flower = Optional.empty();
        Optional<Bouquet> bouquet;

        try {
            String query = fetchMode == FetchMode.EAGER ? SQL_READ_EAGER : SQL_READ_LAZY;

            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setObject(1, id);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                FlowerDto flowerDto = getFlowerDto(rs);
                flower = DtoMapper.getPojo(flowerDto);

                if (fetchMode == FetchMode.EAGER) {
                    BouquetDto bouquetDto = getBouquetDto(rs);

                    if(!(bouquetDto == null)) {
                        bouquet = DtoMapper.getPojo(bouquetDto);

                        if (flower.isPresent() && bouquet.isPresent()) {
                            flower.get().setBouquet(bouquet.get());
                        }
                    }
                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return flower;

    }

    @Override
    public Optional<GeneralFlower> readFirst() {

        Optional<GeneralFlower> flower = Optional.empty();

        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(SQL_READ_FIRST);

            if (rs.next()) {
                FlowerDto flowerDto = getFlowerDto(rs);
                flower = DtoMapper.getPojo(flowerDto);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return flower;
    }

    @Override
    public boolean update(GeneralFlower flower) {

        boolean updated = false;

        try {
            PreparedStatement statement = connection.prepareStatement(SQL_UPDATE);
            statement.setObject(1, flower.getName());
            statement.setObject(2, flower.getLenght());
            statement.setObject(3, flower.getFreshness().getFreshness());
            statement.setObject(4, flower.getPrice());

            Bouquet bouquet = flower.getBouquet();
            if(!(bouquet==null)){
                statement.setObject(7, bouquet.getId());
            }
            statement.setObject(8, flower.getId());

            if (flower instanceof Rose) {
                statement.setObject(5, null);
                statement.setObject(6, ((Rose) flower).isSpike());
            } else if (flower instanceof Chamomile) {
                statement.setObject(5, ((Chamomile) flower).getPetals());
                statement.setObject(6, null);
            }

            updated = statement.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return updated;
    }

    @Override
    public boolean delete(GeneralFlower flower) {

        boolean deleted = false;

        try {
            PreparedStatement statement = connection.prepareStatement(SQL_DELETE);
            statement.setObject(1, flower.getId());
            deleted = statement.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return deleted;

    }

    @Override
    public boolean deleteAll() {

        boolean deleted = false;

        try {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(SQL_DELETE_ALL);
            deleted = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return deleted;
    }

    @Override
    public List<GeneralFlower> findAll(FetchMode fetchMode) {

        List<GeneralFlower> flowerList = new ArrayList<>();

        try {
            String query = fetchMode == FetchMode.EAGER ? SQL_FIND_ALL_EAGER : SQL_FIND_ALL_LAZY;
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                FlowerDto flowerDto = getFlowerDto(rs);
                Optional<GeneralFlower> flowerOpt = DtoMapper.getPojo(flowerDto);
                if(flowerOpt.isPresent()){
                    GeneralFlower flower = flowerOpt.get();
                    flowerList.add(flower);

                    if (fetchMode == FetchMode.EAGER) {
                        BouquetDto bouquetDto = getBouquetDto(rs);
                        if(!(bouquetDto == null)) {
                            Optional<Bouquet> bouquetOpt = DtoMapper.getPojo(bouquetDto);
                            bouquetOpt.ifPresent(flower::setBouquet);
                        }
                    }

                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return flowerList;
    }

    public FlowerDto getFlowerDto(ResultSet rs) throws SQLException {

        FlowerDto dto = new FlowerDto();
        dto.setName(rs.getString("name"));
        dto.setId(rs.getInt("id"));
        dto.setLenght(rs.getInt("lenght"));
        dto.setPrice(rs.getInt("price"));
        dto.setFreshness(rs.getInt("freshness"));
        dto.setSpike(rs.getBoolean("spike"));
        dto.setPetals(rs.getInt("petals"));

        return dto;

    }

    private BouquetDto getBouquetDto(ResultSet rs) throws SQLException {

        if(rs.getInt("bouquet_id") == 0){
            return null;
        }
        BouquetDto dto = new BouquetDto();
        dto.setName(rs.getString("bouquet_name"));
        dto.setId(rs.getInt("bouquet_id"));
        dto.setAssemblePrice(rs.getFloat("bouquet_assemble_price"));

        return dto;

    }

}
