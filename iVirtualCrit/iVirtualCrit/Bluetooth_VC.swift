//
//  Bluetooth_VC.swift
//  iVirtualCrit
//
//  Created by aaronep on 1/28/18.
//  Copyright © 2018 aaronep. All rights reserved.
//





import UIKit
import CoreBluetooth

var maxHRvalue: Int = 185
var usingBTforSpeed: Bool = false
var usingBTforCadence: Bool = false
var usingBTforHeartrate: Bool = false

class Bluetooth_VC: UIViewController, CBCentralManagerDelegate, CBPeripheralDelegate, UITableViewDataSource, UITableViewDelegate {
    
    var centralManager: CBCentralManager!
    var arrPeripheral = [CBPeripheral?]()
    var arr_connected_peripherals = [CBPeripheral?]()
    
    
    let HR_Service = "0x180D"
    let HR_Char =  "0x2A37"
    let CSC_Service = "0x1816"
    let CSC_Char = "0x2A5B"
    
    @IBOutlet weak var BLTE_tableViewOutlet: UITableView!
    @IBOutlet weak var out_Btn1: UIButton!
    @IBOutlet weak var out_Btn2: UIButton!
    @IBOutlet weak var out_Btn3: UIButton!
    
    
    
    @IBAction func act_Btn1(_ sender: UIButton) {
        startScanning()
    }
    @IBAction func act_Btn2(_ sender: UIButton) {
        //startScanning()
    }
    @IBAction func act_Btn3(_ sender: UIButton) {
        //startScanning()
    }
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return arrPeripheral.count
    }
    
    
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        print("Peripheral Connected!!!")
        arr_connected_peripherals.append(peripheral)
        peripheral.delegate = self
        print("Looking for Services for \(String(describing: peripheral.name))...")
        peripheral.discoverServices([CBUUID.init(string: HR_Service), CBUUID.init(string: CSC_Service)])
        self.BLTE_tableViewOutlet.reloadData()
    }
    
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        print("Discovered Services!!!")
        if let services = peripheral.services {
            for service in services {
                print("Discovered service \(service)")
                if (service.uuid == CBUUID(string: HR_Service)) {
                    let transferCharacteristicUUID = CBUUID.init(string: HR_Char)
                    peripheral.discoverCharacteristics([transferCharacteristicUUID], for: service)
                }
                
                if (service.uuid == CBUUID(string: CSC_Service)) {
                    let transferCharacteristicUUID = CBUUID.init(string: CSC_Char)
                    peripheral.discoverCharacteristics([transferCharacteristicUUID], for: service)
                }
            }
        }
    }
    
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        if error != nil {
            print("Error discovering characteristics: \(String(describing: error?.localizedDescription))")
            return
        }
        if let characteristics = service.characteristics {
            for characteristic in characteristics {
                if characteristic.uuid == CBUUID(string: HR_Char) {
                    peripheral.setNotifyValue(true, for: characteristic)
                    print("didDiscoverChar HR for \(peripheral.name!)")
                }
                if characteristic.uuid == CBUUID(string: CSC_Char) {
                    peripheral.setNotifyValue(true, for: characteristic)
                    print("didDiscoverChar CSC for \(peripheral.name!)")
                }
            }
        }
    }
    
    
    
    func centralManager(_ central: CBCentralManager, didDiscover peripheral:
        CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        
        print("Discovered Peripheral")
        print("didDiscover peripheral \(String(describing: peripheral.name)) at \(RSSI)")
        // check to see if we've already saved a reference to this peripheral
        if let firstSuchElement = arrPeripheral.first(where: { $0 == peripheral }) {
            print("\(String(describing: firstSuchElement?.name)) exists")
        } else {
            found_peripheral = peripheral
            arrPeripheral.append(peripheral)
            self.BLTE_tableViewOutlet.reloadData()
        }
    }
    var scanInProgress: Bool = false
    func startScanning() {
        print("Started Scanning")
        scanInProgress = true
        if centralManager.isScanning {
            print("Central Manager is already scanning!!")
            return
        } else {
            self.centralManager.scanForPeripherals(withServices: [CBUUID.init(string: CSC_Service), CBUUID.init(string: HR_Service)], options: [CBCentralManagerScanOptionAllowDuplicatesKey:true])
        }
        
        self.out_Btn1.setTitle("...", for: .normal)
        DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(6), execute: {
            self.centralManager.stopScan()
            print("Stop Scanning")
            self.out_Btn1.setTitle("RESCAN", for: .normal)
            
            DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(1), execute: {
                self.BLTE_tableViewOutlet.reloadData()
                self.scanInProgress = false
            })
        })
        
    }
    
    
    //var hr: String = "0"
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        if error != nil {
            print("Error updating value for characteristic: \(characteristic) - \(String(describing: error?.localizedDescription))")
            return
        }
        guard characteristic.value != nil else {
            return
        }
        
        func decodeHRValue(withData data: Data) {
            let count = data.count / MemoryLayout<UInt8>.size
            var array = [UInt8](repeating: 0, count: count)
            (data as NSData).getBytes(&array, length:count * MemoryLayout<UInt8>.size)
            var bpmValue : Int = 0
            if ((array[0] & 0x01) == 0) {
                bpmValue = Int(array[1])
                //hr = stringer(dbl: Double(bpmValue), len: 0)
            } else {
                bpmValue = Int(UInt16(array[2] * 0xFF) + UInt16(array[1]))
                //hr = stringer(dbl: Double(bpmValue), len: 0)
            }
            usingBTforHeartrate = true
            current.currentHR = bpmValue
            current.currentScore = getScoreFromHR(x: Double(current.currentHR))
            //let str: String = "\(current.currentHR):\(stringer(dbl: current.currentScore, len: 0))"
            //out_Btn1.setTitle(str, for: .normal)
        }
        
        
        func decodeCSC(withData data : Data) {
            let WHEEL_REVOLUTION_FLAG               : UInt8 = 0x01
            let CRANK_REVOLUTION_FLAG               : UInt8 = 0x02
            let value = UnsafeMutablePointer<UInt8>(mutating: (data as NSData).bytes.bindMemory(to: UInt8.self, capacity: data.count))
            let flag = value[0]
            if flag & WHEEL_REVOLUTION_FLAG == 1 {
                //print("SPD value[1]");print(value[1])
                if value[1] > 0 {
                  //out_Btn2.setTitle(String(value[1]), for: .normal)
                    usingBTforSpeed = true
                }
                processWheelData(withData: data)
                if flag & CRANK_REVOLUTION_FLAG == 2 {
                    if value[7] > 0 {
                        //out_Btn3.setTitle(String(value[7]), for: .normal)
                        usingBTforCadence = true
                    }
                    //print("CAD value[7]");print(value[7])
                    processCrankData(withData: data, andCrankRevolutionIndex: 7)
                }
            } else {
                if flag & CRANK_REVOLUTION_FLAG == 2 {
                    if value[1] > 0 {
                        //out_Btn3.setTitle(String(value[1]), for: .normal)
                        usingBTforCadence = true
                    }
                    //print("CAD value[1]");print(value[1])
                    processCrankData(withData: data, andCrankRevolutionIndex: 1)
                }
            }
        }
        
        
        
        if characteristic.uuid == CBUUID(string: HR_Char) {
            guard characteristic.value != nil else {
                print("Characteristic Value is nil on this go-round")
                return
            }
            if error != nil {
                print("Error updating value for characteristic: \(characteristic) - \(String(describing: error?.localizedDescription))")
                return
            }
            decodeHRValue(withData: characteristic.value!)
        }
        
        if characteristic.uuid == CBUUID(string: CSC_Char) {
            guard characteristic.value != nil else {
                print("Characteristic Value is nil on this go-round")
                return
            }
            
            if error != nil {
                print("Error updating value for characteristic: \(characteristic) - \(String(describing: error?.localizedDescription))")
                return
            }
            
            decodeCSC(withData: characteristic.value!)
        }
    }
    
    func disconnectAllPeripherals() {
        dump(arrPeripheral)
        for p in arrPeripheral {
            // verify we have a peripheral
            
            // check to see if the peripheral is connected
            if p?.state != .connected {
                print("Peripheral exists but is not connected.")
                return
            }
            guard let services = p?.services else {
                print("Cancel Peripheral Connection")
                centralManager.cancelPeripheralConnection(p!)  //no services
                return
            }
            for service in services {
                // iterate through characteristics
                if let characteristics = service.characteristics {
                    for characteristic in characteristics {
                        // find the Transfer Characteristic we defined in our Device struct
                        if characteristic.uuid == CBUUID.init(string: CSC_Char) {
                            p?.setNotifyValue(false, for: characteristic)
                            print("set Notify Value to False for:  \(String(describing: p?.name))")
                            return
                        }
                        if characteristic.uuid == CBUUID.init(string: HR_Char) {
                            p?.setNotifyValue(false, for: characteristic)
                            print("set Notify Value to False for:  \(String(describing: p?.name))")
                            return
                        }
                    }
                }
            }
            // disconnect from the peripheral
            centralManager.cancelPeripheralConnection(p!)
        }
        arr_connected_peripherals = []
    }
    
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?){
        print("Did Disconnect Peripheral")        
        // check to see if the peripheral is connected
        print("did disconnect peripheral:  \(String(describing: peripheral.name))")
        if peripheral.state != .connected {
            print("Peripheral exists but is not connected.")
            //put rescan code here
            centralManager.connect(peripheral, options: nil)
            self.BLTE_tableViewOutlet.reloadData()
            return
        }
        
        guard let services = peripheral.services else {
            // disconnect directly
            centralManager.cancelPeripheralConnection(peripheral)
            print("Cancel Peripheral Connection")
            self.BLTE_tableViewOutlet.reloadData()
            return
        }
        
        for service in services {
            // iterate through characteristics
            if let characteristics = service.characteristics {
                for characteristic in characteristics {
                    
                    if characteristic.uuid == CBUUID.init(string: HR_Char) {
                        peripheral.setNotifyValue(false, for: characteristic)
                        print("set Notify Value to False")
                        return
                    }
                    if characteristic.uuid == CBUUID.init(string: CSC_Char) {
                        peripheral.setNotifyValue(false, for: characteristic)
                        print("set Notify Value to False")
                        return
                    }
                    
                }
            }
        }
        centralManager.cancelPeripheralConnection(peripheral)
        print("Cancel Connection")
        self.BLTE_tableViewOutlet.reloadData()
    }
    
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "R1") as! BLTE_TableViewCell
        
        let str = arrPeripheral[indexPath.row]?.name
        var strr: String = String(describing: arrPeripheral[indexPath.row]!.identifier)
        
        switch(arrPeripheral[indexPath.row]!.state) {
        case .connected:
            strr = "Connected"
        case .disconnected:
            strr = "Disconnected"
        case .connecting:
            strr = "Connecting"
        case .disconnecting:
            strr = "Disconnecting"
        }
        
        cell.BLTE_CellTitle.text = str
        cell.BLTE_CellSubTitle.text = String(describing: strr)
        
        return cell
    }
    
    
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        centralManager?.connect(arrPeripheral[indexPath.row]!, options: nil)
        tableView.deselectRow(at: indexPath, animated: true)
        self.BLTE_tableViewOutlet.reloadData()
    }
    
    var found_peripheral: CBPeripheral?
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        print("Central Manager State Updated: \(central.state)")
        
        switch (central.state) {
        case .unknown:
            print("state: unknown")
            break;
        case .resetting:
            print("state: resetting")
            break;
        case .unsupported:
            print("state: unsupported")
            break;
        case .unauthorized:
            print("state: unauthorized")
            break;
        case .poweredOff:
            print("state: power off")
            break;
        case .poweredOn:
            print("state: power on")
            found_peripheral = nil
            break;
        }
        
        
        self.BLTE_tableViewOutlet.reloadData()
    }
    
    
    
    
    
    override func viewDidLoad() {
        super.viewDidLoad()

        centralManager = CBCentralManager(delegate: self, queue: nil)
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */

}
