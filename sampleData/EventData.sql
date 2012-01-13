-- MySQL dump 10.13  Distrib 5.1.30, for apple-darwin9.4.0 (i386)
--
-- Host: localhost    Database: gts
-- ------------------------------------------------------
-- Server version	5.1.30

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `EventData`
--

DROP TABLE IF EXISTS `EventData`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `EventData` (
  `accountID` varchar(32) NOT NULL,
  `deviceID` varchar(32) NOT NULL,
  `timestamp` int(10) unsigned NOT NULL,
  `statusCode` int(10) unsigned NOT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `gpsAge` int(10) unsigned DEFAULT NULL,
  `speedKPH` double DEFAULT NULL,
  `heading` double DEFAULT NULL,
  `altitude` double DEFAULT NULL,
  `transportID` varchar(32) DEFAULT NULL,
  `inputMask` int(10) unsigned DEFAULT NULL,
  `address` varchar(90) CHARACTER SET utf8 DEFAULT NULL,
  `dataSource` varchar(32) DEFAULT NULL,
  `rawData` text,
  `distanceKM` double DEFAULT NULL,
  `odometerKM` double DEFAULT NULL,
  `geozoneIndex` int(10) unsigned DEFAULT NULL,
  `geozoneID` varchar(32) DEFAULT NULL,
  `creationTime` int(10) unsigned DEFAULT NULL,
  `streetAddress` varchar(90) CHARACTER SET utf8 DEFAULT NULL,
  `city` varchar(40) CHARACTER SET utf8 DEFAULT NULL,
  `stateProvince` varchar(40) CHARACTER SET utf8 DEFAULT NULL,
  `postalCode` varchar(16) CHARACTER SET utf8 DEFAULT NULL,
  `country` varchar(40) CHARACTER SET utf8 DEFAULT NULL,
  `subdivision` varchar(32) CHARACTER SET utf8 DEFAULT NULL,
  `gpsFixType` smallint(5) unsigned DEFAULT NULL,
  `horzAccuracy` double DEFAULT NULL,
  `HDOP` double DEFAULT NULL,
  `satelliteCount` smallint(5) unsigned DEFAULT NULL,
  `batteryLevel` double DEFAULT NULL,
  `entity` varchar(32) CHARACTER SET utf8 DEFAULT NULL,
  `driver` varchar(32) CHARACTER SET utf8 DEFAULT NULL,
  `topSpeedKPH` double DEFAULT NULL,
  `sensorLow` int(10) unsigned DEFAULT NULL,
  `sensorHigh` int(10) unsigned DEFAULT NULL,
  `dataPush` tinyint(4) DEFAULT NULL,
  `costCenter` int(10) unsigned DEFAULT NULL,
  `fuelLevel` double DEFAULT NULL,
  `fuelEconomy` double DEFAULT NULL,
  `fuelTotal` double DEFAULT NULL,
  `fuelIdle` double DEFAULT NULL,
  `engineRpm` int(10) unsigned DEFAULT NULL,
  `coolantLevel` double DEFAULT NULL,
  `coolantTemp` double DEFAULT NULL,
  `brakeGForce` double DEFAULT NULL,
  `j1708Fault` int(10) unsigned DEFAULT NULL,
  `thermoAverage0` double DEFAULT NULL,
  `thermoAverage1` double DEFAULT NULL,
  `thermoAverage2` double DEFAULT NULL,
  `thermoAverage3` double DEFAULT NULL,
  `vertAccuracy` double DEFAULT NULL,
  `speedLimitKPH` double DEFAULT NULL,
  `isTollRoad` tinyint(4) DEFAULT NULL,
  `entityID` varchar(32) CHARACTER SET utf8 DEFAULT NULL,
  `driverID` varchar(32) CHARACTER SET utf8 DEFAULT NULL,
  `driverMessage` varchar(200) CHARACTER SET utf8 DEFAULT NULL,
  `jobNumber` varchar(32) DEFAULT NULL,
  `oilPressure` double DEFAULT NULL,
  `ptoEngaged` tinyint(4) DEFAULT NULL,
  `engineHours` double DEFAULT NULL,
  `engineLoad` double DEFAULT NULL,
  `idleHours` double DEFAULT NULL,
  `transOilTemp` double DEFAULT NULL,
  `brakePressure` double DEFAULT NULL,
  `ptoHours` double DEFAULT NULL,
  `vBatteryVolts` double DEFAULT NULL,
  `oilLevel` double DEFAULT NULL,
  `fuelUsage` double DEFAULT NULL,
  `fuelPTO` double DEFAULT NULL,
  `dayEngineStarts` smallint(5) unsigned DEFAULT NULL,
  `dayIdleHours` double DEFAULT NULL,
  `dayFuelIdle` double DEFAULT NULL,
  `dayWorkHours` double DEFAULT NULL,
  `dayFuelWork` double DEFAULT NULL,
  `dayFuelPTO` double DEFAULT NULL,
  `dayDistanceKM` double DEFAULT NULL,
  `dayFuelTotal` double DEFAULT NULL,
  `mobileCountryCode` int(11) DEFAULT NULL,
  `mobileNetworkCode` int(11) DEFAULT NULL,
  `cellTimingAdvance` int(11) DEFAULT NULL,
  `cellLatitude` double DEFAULT NULL,
  `cellLongitude` double DEFAULT NULL,
  `cellServingInfo` varchar(64) DEFAULT NULL,
  `cellNeighborInfo0` varchar(64) DEFAULT NULL,
  `cellNeighborInfo1` varchar(64) DEFAULT NULL,
  `cellNeighborInfo2` varchar(64) DEFAULT NULL,
  `cellNeighborInfo3` varchar(64) DEFAULT NULL,
  `cellNeighborInfo4` varchar(64) DEFAULT NULL,
  `cellNeighborInfo5` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`accountID`,`deviceID`,`timestamp`,`statusCode`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
SET character_set_client = @saved_cs_client;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2010-05-29 17:28:36
