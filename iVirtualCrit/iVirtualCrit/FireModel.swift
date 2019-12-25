//
//  FireModel.swift
//  iVirtualCrit
//
//  Created by aaronep on 2/2/18.
//  Copyright © 2018 aaronep. All rights reserved.
//

import Foundation
import AVFoundation

//var str = "Hi, this is Kazumi. Let's get started"
//func newSpeakerWithClass() {
//    let str = self.str
//    let x = TextToSpeechUtils.init()
//    x.synthesizeSpeech(forText: str)
//}

//Utils.shared.say(sentence: "Thanks Casey!")


class Utils: NSObject {
    static let shared = Utils()
    
    let synth = AVSpeechSynthesizer()
    let audioSession = AVAudioSession.sharedInstance()
    
    override init() {
        super.init()
        
        synth.delegate = self
    }
    
    func say(sentence: String) {
        do {
            try audioSession.setCategory(AVAudioSessionCategoryPlayback, with: AVAudioSessionCategoryOptions.duckOthers)
            
            let utterance = AVSpeechUtterance(string: sentence)
            
            try audioSession.setActive(true)
            
            synth.speak(utterance)
        } catch {
            print("Uh oh!")
        }
    }
}

extension Utils: AVSpeechSynthesizerDelegate {
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        do {
            try audioSession.setActive(false)
        } catch {
            print("Uh oh!")
        }
    }
}

//
//
//class TextToSpeechUtils: NSObject, AVSpeechSynthesizerDelegate {
//
//    let synthesizer = AVSpeechSynthesizer()
//    let audioSession = AVAudioSession.sharedInstance()
//    let defaultLanguage = "en-US"
//    var lastPlayingUtterance: AVSpeechUtterance?
//
//    public func synthesizeSpeech(forText text: String) {
//            if (text.isEmpty) { return }
//            do {
//                try audioSession.setCategory(AVAudioSessionCategoryPlayback, with: [.duckOthers])
//                try audioSession.setActive(true)
//            } catch {
//                return
//            }
//
//            let utterance = AVSpeechUtterance(string:text)
//            utterance.rate = AVSpeechUtteranceDefaultSpeechRate
//            utterance.volume = 1.0
//            utterance.voice = AVSpeechSynthesisVoice(language: defaultLanguage)
//            self.synthesizer.speak(utterance)
//
////            DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(10), execute: {
////                do {
////                    try self.audioSession.setActive(false)
////                } catch {
////                    print("err closing audio session")
////                }
////            })
//
//            self.lastPlayingUtterance = utterance
//
//    }
//
//    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
//        if (synthesizer == self.synthesizer && self.lastPlayingUtterance == utterance) {
//            do {
//                // after last utterance has played - deactivate the audio session
//                try self.audioSession.setActive(false);
//            } catch {
//                return
//            }
//        }
//    }
//
//}

