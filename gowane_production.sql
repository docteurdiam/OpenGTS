-- manque 'password'/ email as key
CREATE TABLE Account (
  accountID character varying(32) NOT NULL,
  accountType smallint DEFAULT NULL,
  notifyEmail character varying(128) DEFAULT NULL,
  speedUnits integer DEFAULT NULL,
  distanceUnits integer DEFAULT NULL,
  volumeUnits integer DEFAULT NULL,
  pressureUnits integer DEFAULT NULL,
  economyUnits integer DEFAULT NULL,
  temperatureUnits integer DEFAULT NULL,
  latLonFormat integer DEFAULT NULL,
  geocoderMode integer DEFAULT NULL,
  privateLabelName character varying(32) DEFAULT NULL,
  isBorderCrossing integer DEFAULT NULL,
  retainedEventAge integer DEFAULT NULL,
  maximumDevices integer DEFAULT NULL,
  totalPingCount smallint DEFAULT NULL,
  maxPingCount smallint DEFAULT NULL,
  autoAddDevices integer DEFAULT NULL,
  dcsPropertiesID varchar(32) DEFAULT NULL,
  expirationTime integer DEFAULT NULL,
  defaultUser character varying(32) DEFAULT NULL,
  contactName character varying(64) DEFAULT NULL,
  contactPhone character varying(32) DEFAULT NULL,
  contactEmail character varying(128) DEFAULT NULL,
  timeZone character varying(32) DEFAULT NULL,
  passwdQueryTime integer DEFAULT NULL,
  lastLoginTime integer DEFAULT NULL,
  isActive integer DEFAULT NULL,
  displayName character varying(40) DEFAULT NULL,
  description character varying(128) DEFAULT NULL,
  notes text,
  lastUpdateTime int DEFAULT NULL,
  creationTime int DEFAULT NULL,
  PRIMARY KEY (accountID)
)


CREATE TABLE AccountString (
  accountID character varying(32) NOT NULL,
  stringID character varying(32) NOT NULL,
  singularTitle character varying(64) DEFAULT NULL,
  pluralTitle character varying(64) DEFAULT NULL,
  description character varying(128) DEFAULT NULL,
  lastUpdateTime integer DEFAULT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (accountID,stringID)
) 

-- manque 'timestamp' et key
CREATE TABLE Device (
  accountID character varying(32) NOT NULL,
  deviceID character varying(32) NOT NULL,
  groupID character varying(32) DEFAULT NULL,
  equipmentType character varying(40) DEFAULT NULL,
  vehicleID character varying(24) DEFAULT NULL,
  licensePlate character varying(24) DEFAULT NULL,
  driverID character varying(32) DEFAULT NULL,
  fuelCapacity numeric DEFAULT NULL,
  fuelEconomy numeric DEFAULT NULL,
  speedLimitKPH numeric DEFAULT NULL,
  expirationTime integer  DEFAULT NULL,
  uniqueID character varying(40) DEFAULT NULL,
  deviceCode character varying(24) DEFAULT NULL,
  deviceType character varying(24) DEFAULT NULL,
  dcsPropertiesID character varying(32) DEFAULT NULL,
  pushpinID character varying(32) DEFAULT NULL,
  displayColor character varying(16) DEFAULT NULL,
  serialNumber character varying(24) DEFAULT NULL,
  simPhoneNumber character varying(24) DEFAULT NULL,
  smsEmail character varying(64) DEFAULT NULL,
  imeiNumber character varying(24) DEFAULT NULL,
  dataKey text,
  ignitionIndex smallint DEFAULT NULL,
  codeVersion character varying(32) DEFAULT NULL,
  featureSet character varying(64) DEFAULT NULL,
  ipAddressValid character varying(128) DEFAULT NULL,
  lastTotalConnectTime integer  DEFAULT NULL,
  lastDuplexConnectTime integer  DEFAULT NULL,
  pendingPingCommand text,
  lastPingTime integer  DEFAULT NULL,
  totalPingCount smallint  DEFAULT NULL,
  maxPingCount smallint  DEFAULT NULL,
  expectAck smallint DEFAULT NULL,
  lastAckCommand text,
  lastAckTime integer  DEFAULT NULL,
  dcsConfigMask integer  DEFAULT NULL,
  supportsDMTP smallint DEFAULT NULL,
  supportedEncodings smallint  DEFAULT NULL,
  unitLimitInterval smallint  DEFAULT NULL,
  maxAllowedEvents smallint  DEFAULT NULL,
  totalProfileMask oid,
  totalMaxConn smallint  DEFAULT NULL,
  totalMaxConnPerMin smallint  DEFAULT NULL,
  duplexProfileMask oid,
  duplexMaxConn smallint  DEFAULT NULL,
  duplexMaxConnPerMin smallint  DEFAULT NULL,
  ipAddressCurrent character varying(32) DEFAULT NULL,
  remotePortCurrent smallint  DEFAULT NULL,
  listenPortCurrent smallint  DEFAULT NULL,
  lastInputState integer  DEFAULT NULL,
  lastBatteryLevel numeric DEFAULT NULL,
  lastFuelLevel numeric DEFAULT NULL,
  lastFuelTotal numeric DEFAULT NULL,
  lastOilLevel numeric DEFAULT NULL,
  lastValidLatitude numeric DEFAULT NULL,
  lastValidLongitude numeric DEFAULT NULL,
  lastValidHeading numeric DEFAULT NULL,
  lastGPSTimestamp integer  DEFAULT NULL,
  lastCellServingInfo character varying(100) DEFAULT NULL,
  lastOdometerKM numeric DEFAULT NULL,
  odometerOffsetKM numeric DEFAULT NULL,
  lastEngineHours numeric DEFAULT NULL,
  isActive smallint DEFAULT NULL,
  displayName character varying(40) DEFAULT NULL,
  description character varying(128) DEFAULT NULL,
  notes text,
  lastUpdateTime integer  DEFAULT NULL,
  creationTime integer  DEFAULT NULL,
  PRIMARY KEY (accountID,deviceID)
)


CREATE TABLE DeviceGroup (
  accountID character varying(32) NOT NULL,
  groupID character varying(32) NOT NULL,
  displayName character varying(40) DEFAULT NULL,
  description character varying(128) DEFAULT NULL,
  notes text,
  lastUpdateTime integer DEFAULT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (accountID,groupID)
) 


CREATE TABLE DeviceList (
  accountID character varying(32) NOT NULL,
  groupID character varying(32) NOT NULL,
  deviceID character varying(32) NOT NULL,
  lastUpdateTime integer DEFAULT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (accountID,groupID,deviceID)
) 


-- manque 'timestamp'
CREATE TABLE Diagnostic (
  accountID character varying(32) NOT NULL,
  deviceID character varying(32) NOT NULL,
  isError smallint NOT NULL,
  codeKey integer NOT NULL,
  binaryValue oid,
  PRIMARY KEY (accountID,deviceID,isError,codeKey)
) 


CREATE TABLE Driver (
  accountID character varying(32) NOT NULL,
  driverID character varying(32) NOT NULL,
  contactPhone character varying(32) DEFAULT NULL,
  contactEmail character varying(128) DEFAULT NULL,
  licenseType character varying(24) DEFAULT NULL,
  licenseNumber character varying(32) DEFAULT NULL,
  licenseExpire integer DEFAULT NULL,
  badgeID character varying(32) DEFAULT NULL,
  address character varying(90) DEFAULT NULL,
  birthdate integer DEFAULT NULL,
  deviceID character varying(32) DEFAULT NULL,
  displayName character varying(40) DEFAULT NULL,
  description character varying(128) DEFAULT NULL,
  notes text,
  lastUpdateTime integer DEFAULT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (accountID,driverID)
) 


-- manque 'timestamp'
CREATE TABLE EventData (
  accountID varchar(32) NOT NULL,
  deviceID character varying(32) NOT NULL,
  statusCode integer NOT NULL,
  latitude numeric DEFAULT NULL,
  longitude numeric DEFAULT NULL,
  gpsAge integer DEFAULT NULL,
  speedKPH numeric DEFAULT NULL,
  heading numeric DEFAULT NULL,
  altitude numeric DEFAULT NULL,
  transportID character varying(32) DEFAULT NULL,
  inputMask integer DEFAULT NULL,
  address character varying(255) DEFAULT NULL,
  dataSource character varying(32) DEFAULT NULL,
  rawData text,
  distanceKM numeric DEFAULT NULL,
  odometerKM numeric DEFAULT NULL,
  geozoneIndex integer DEFAULT NULL,
  geozoneID character varying(32) DEFAULT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (accountID,deviceID,statusCode)
)


CREATE TABLE EventTemplate (
  accountID character varying(32) NOT NULL,
  deviceID character varying(32) NOT NULL,
  customType smallint NOT NULL,
  repeatLast smallint DEFAULT NULL,
  template text,
  PRIMARY KEY (accountID,deviceID,customType)
) 

-- key bounds, altIndex
CREATE TABLE Geozone (
  accountID character varying(32) NOT NULL,
  geozoneID character varying(32) NOT NULL,
  sortID integer NOT NULL,
  minLatitude numeric DEFAULT NULL,
  maxLatitude numeric DEFAULT NULL,
  minLongitude numeric DEFAULT NULL,
  maxLongitude numeric DEFAULT NULL,
  reverseGeocode smallint DEFAULT NULL,
  arrivalZone smallint DEFAULT NULL,
  departureZone smallint DEFAULT NULL,
  autoNotify smallint DEFAULT NULL,
  zoomRegion smallint DEFAULT NULL,
  shapeColor character varying(12) DEFAULT NULL,
  zoneType smallint DEFAULT NULL,
  radius integer DEFAULT NULL,
  latitude1 numeric DEFAULT NULL,
  longitude1 numeric DEFAULT NULL,
  latitude2 numeric DEFAULT NULL,
  longitude2 numeric DEFAULT NULL,
  latitude3 numeric DEFAULT NULL,
  longitude3 numeric DEFAULT NULL,
  latitude4 numeric DEFAULT NULL,
  longitude4 numeric DEFAULT NULL,
  latitude5 numeric DEFAULT NULL,
  longitude5 numeric DEFAULT NULL,
  latitude6 numeric DEFAULT NULL,
  longitude6 numeric DEFAULT NULL,
  latitude7 numeric DEFAULT NULL,
  longitude7 numeric DEFAULT NULL,
  latitude8 numeric DEFAULT NULL,
  longitude8 numeric DEFAULT NULL,
  clientUpload smallint DEFAULT NULL,
  clientID integer DEFAULT NULL,
  streetAddress character varying(90) DEFAULT NULL,
  city character varying(40) DEFAULT NULL,
  stateProvince character varying(40) DEFAULT NULL,
  postalCode character varying(16) DEFAULT NULL,
  country character varying(40) DEFAULT NULL,
  subdivision character varying(32) DEFAULT NULL,
  displayName character varying(40) DEFAULT NULL,
  description character varying(128) DEFAULT NULL,
  lastUpdateTime integer DEFAULT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (accountID,geozoneID,sortID)
)


CREATE TABLE GroupList (
  accountID character varying(32) NOT NULL,
  userID character varying(32) NOT NULL,
  groupID character varying(32) NOT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (accountID,userID,groupID)
) 


CREATE TABLE PendingPacket (
  accountID character varying(32) NOT NULL,
  deviceID character varying(32) NOT NULL,
  queueTime integer NOT NULL,
  sequence smallint NOT NULL,
  packetBytes oid,
  autoDelete smallint DEFAULT NULL,
  PRIMARY KEY (accountID,deviceID,queueTime,sequence)
)


-- manque 'timestamp'
CREATE TABLE Property (
  accountID character varying(32) NOT NULL,
  deviceID character varying(32) NOT NULL,
  propKey integer NOT NULL,
  binaryValue oid,
  PRIMARY KEY (accountID,deviceID,propKey)
) 


-- manque 'timstamp'
CREATE TABLE Resource (
  accountID character varying(32) NOT NULL,
  resourceID character varying(64) NOT NULL,
  type character varying(16) DEFAULT NULL,
  title character varying(70) DEFAULT NULL,
  properties text,
  value oid,
  displayName character varying(40) DEFAULT NULL,
  description character varying(128) DEFAULT NULL,
  lastUpdateTime integer DEFAULT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (accountID,resourceID)
) 



CREATE TABLE Role (
  accountID character varying(32) NOT NULL,
  roleID character varying(32) NOT NULL,
  displayName character varying(40) DEFAULT NULL,
  description character varying(128) DEFAULT NULL,
  notes text,
  lastUpdateTime integer DEFAULT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (accountID,roleID)
) 


CREATE TABLE RoleAcl (
  accountID character varying(32) NOT NULL,
  roleID character varying(32) NOT NULL,
  aclID character varying(64) NOT NULL,
  accessLevel smallint DEFAULT NULL,
  description character varying(128) DEFAULT NULL,
  lastUpdateTime integer DEFAULT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (accountID,roleID,aclID)
) 


CREATE TABLE StatusCode (
  accountID character varying(32) NOT NULL,
  deviceID character varying(32) NOT NULL,
  statusCode integer NOT NULL,
  statusName character varying(18) DEFAULT NULL,
  foregroundColor character varying(10) DEFAULT NULL,
  backgroundColor character varying(10) DEFAULT NULL,
  iconSelector character varying(128) DEFAULT NULL,
  iconName character varying(24) DEFAULT NULL,
  description character varying(128) DEFAULT NULL,
  lastUpdateTime integer DEFAULT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (accountID,deviceID,statusCode)
) 


CREATE TABLE SystemProps (
  propertyID character varying(32) NOT NULL,
  value text,
  description character varying(128) DEFAULT NULL,
  lastUpdateTime integer DEFAULT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (propertyID)
) 


-- manque les keys
CREATE TABLE Transport (
  accountID character varying(32) NOT NULL,
  transportID character varying(32) NOT NULL,
  assocAccountID character varying(32) DEFAULT NULL,
  assocDeviceID character varying(32) DEFAULT NULL,
  uniqueID character varying(40) DEFAULT NULL,
  deviceCode character varying(24) DEFAULT NULL,
  deviceType character varying(24) DEFAULT NULL,
  serialNumber character varying(24) DEFAULT NULL,
  simPhoneNumber character varying(24) DEFAULT NULL,
  smsEmail character varying(64) DEFAULT NULL,
  imeiNumber character varying(24) DEFAULT NULL,
  lastInputState integer DEFAULT NULL,
  ignitionIndex smallint DEFAULT NULL,
  codeVersion character varying(32) DEFAULT NULL,
  featureSet character varying(64) DEFAULT NULL,
  ipAddressValid character varying(128) DEFAULT NULL,
  ipAddressCurrent character varying(32) DEFAULT NULL,
  remotePortCurrent smallint DEFAULT NULL,
  listenPortCurrent smallint DEFAULT NULL,
  pendingPingCommand text,
  lastPingTime integer DEFAULT NULL,
  totalPingCount smallint DEFAULT NULL,
  maxPingCount smallint DEFAULT NULL,
  expectAck smallint DEFAULT NULL,
  lastAckCommand text,
  lastAckTime integer DEFAULT NULL,
  supportsDMTP smallint DEFAULT NULL,
  supportedEncodings smallint DEFAULT NULL,
  unitLimitInterval smallint DEFAULT NULL,
  maxAllowedEvents smallint DEFAULT NULL,
  totalProfileMask oid,
  totalMaxConn smallint DEFAULT NULL,
  totalMaxConnPerMin smallint DEFAULT NULL,
  duplexProfileMask oid,
  duplexMaxConn smallint DEFAULT NULL,
  duplexMaxConnPerMin smallint DEFAULT NULL,
  lastTotalConnectTime integer DEFAULT NULL,
  lastDuplexConnectTime integer DEFAULT NULL,
  displayName character varying(40) DEFAULT NULL,
  description character varying(128) DEFAULT NULL,
  lastUpdateTime integer DEFAULT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (accountID,transportID)
 ) 



CREATE TABLE UniqueXID (
  uniqueID character varying(40) NOT NULL,
  accountID character varying(32) DEFAULT NULL,
  transportID character varying(32) DEFAULT NULL,
  PRIMARY KEY (uniqueID)
)

-- voir le titre User
CREATE TABLE 'User' (
  accountID character varying(32) NOT NULL,
  userID character varying(32) NOT NULL,
  userType smallint DEFAULT NULL,
  roleID character varying(32) DEFAULT NULL,
  password character varying(32) DEFAULT NULL,
  gender smallint  DEFAULT NULL,
  notifyEmail character varying(128) DEFAULT NULL,
  contactName character varying(64) DEFAULT NULL,
  contactPhone character varying(32) DEFAULT NULL,
  contactEmail character varying(64) DEFAULT NULL,
  timeZone character varying(32) DEFAULT NULL,
  firstLoginPageID character varying(24) DEFAULT NULL,
  preferredDeviceID character varying(32) DEFAULT NULL,
  maxAccessLevel smallint  DEFAULT NULL,
  passwdQueryTime integer DEFAULT NULL,
  lastLoginTime integer DEFAULT NULL,
  isActive smallint DEFAULT NULL,
  displayName character varying(40) DEFAULT NULL,
  description character varying(128) DEFAULT NULL,
  notes text,
  lastUpdateTime integer DEFAULT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (accountID,userID),
) 

CREATE TABLE UserAcl (
  accountID character varying(32) NOT NULL,
  userID character varying(32) NOT NULL,
  aclID character varying(64) NOT NULL,
  accessLevel smallint DEFAULT NULL,
  description character varying(128) DEFAULT NULL,
  lastUpdateTime integer DEFAULT NULL,
  creationTime integer DEFAULT NULL,
  PRIMARY KEY (accountID,userID,aclID)
) 

