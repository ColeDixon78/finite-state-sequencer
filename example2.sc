(
SynthDef.new(name:\marimba, ugenGraphFunc:{
    | out, freq = 440, pan = 0, amp = 1, atk = 0.01, sus = 0.01, rel = 1.5 |
    var mod = SinOsc.kr(880, mul: Line.kr(100,0,atk * 3));
    var source = Pulse.ar(
        freq + mod,
        mul: amp + LFTri.kr(Rand(9,17),3)
    ) * Env.linen(atk,sus,rel, curve: -2).ar(doneAction: Done.freeSelf);
    var filtered = LPF.ar(
         source,
        Env.perc(0.01,0.2).kr() * (freq) + (freq / 2)
    );
    var panned = Pan2.ar(filtered, pan);
    Out.ar(bus:out, channelsArray:panned)
}, rates:nil, prependArgs:nil, variants:nil, metadata:nil).add;

SynthDef.new(name:\thwack, ugenGraphFunc:{
    | out, freq = 440, pan = 0, amp = 1, atk = 0.01, dec = 0.1, intensity = 0.1, mod = 3|
    var noise = BPF.ar(
        WhiteNoise.ar(amp),
        freq + SinOsc.kr(mod).range(-200,200),
        XLine.kr(1,0.1,intensity.clip(0,1))
    ) * Env.perc(atk,dec).ar();
    var panned = Pan2.ar(noise, pan);
    Out.ar(bus:out, channelsArray:panned)
}, rates:nil, prependArgs:nil, variants:nil, metadata:nil).add;

SynthDef.new(name:\tick, ugenGraphFunc:{
    | out, freq = 440, pan = 0, amp = 1, atk = 0.03, sus = 0.4, rel = 0.3, curve = -5|
    var source = Impulse.ar(
        Rand(16,24) + SinOsc.kr(1).range(-10,10),
        amp
    ) * Env.linen(atk, sus, rel, curve: curve).ar(doneAction: Done.freeSelf);
    var panned = Pan2.ar(source, pan + LFTri.kr(1));
    Out.ar(bus:out, channelsArray:panned)
}, rates:nil, prependArgs:nil, variants:nil, metadata:nil).add;

SynthDef.new(name:\kick, ugenGraphFunc:{
    | out, freq = 60, pan = 0, amp = 1, atk = 0.001, dur = 0.4, body = 0.1, punch = 0.5|
    var noise =
        BPF.ar(
            WhiteNoise.ar(amp),
            freq * 2,
            XLine.kr(1,0.1,0.1)
        ) * 
        Env.perc(atk,0.05).ar();
    var source =
        FreeVerb.ar(
            SinOsc.ar(XLine.kr(freq * 1.2, freq, 0.01, 1), amp) + noise,
            mix: 0.24,
            room: body
        ) * 
        Env.perc(atk,dur, punch.clip(0,1) * -8).ar(doneAction: Done.freeSelf);
    var panned = Pan2.ar(source, pan);
    Out.ar(bus:out, channelsArray:panned)
}, rates:nil, prependArgs:nil, variants:nil, metadata:nil).add;
)
~stochasticPattern = p({
    | in |
    var currentStateIndex = in[2];
    var states = in[0];
    var stateCount = states.size;
    var transitionMatrix = in[1];
    var indexArray = Array.series(stateCount,0,1);
    loop {
        currentStateIndex = indexArray.wchoose(transitionMatrix[currentStateIndex]);
        states[currentStateIndex].yield;
    };
});

Tdef(\kickPattern, {
    var deltas = [1,1/2,1/2];
    var deltaMat = [
        [0.7,0.3,0],
        [0,0,1],
        [1/2,1/2,0]
    ];
    var deltaStream = ~stochasticPattern.asStream;
    var nextDelta;
    loop {
        nextDelta = deltaStream.next([deltas, deltaMat,0]);
        Server.default.bind { Synth(\kick, [
            \freq: 60,
            \punch: rrand(0.1,0.4),
            \rel: nextDelta * 0.8
        ]) };
        nextDelta.yield;
    }
});

Tdef(\clapPattern, {
    var deltas = [1,2,3];
    var deltaMat = [
        [1/2,1/4,1/4],
        [1/2,1/2,0],
        [1/4,0,3/4]
    ];
    var deltaStream = ~stochasticPattern.asStream;
    var nextDelta;
    loop {
        nextDelta = deltaStream.next([deltas, deltaMat,0]);
        Server.default.bind {
            Synth(\thwack, [
                freq: rrand(800,1200),
                pan: rrand(-0.5,0.5),
                atk: 0.01,
                dec: nextDelta / 2,
                intensity: exprand(0.01,1),
                amp: 0.5
            ]);
        };
        nextDelta.yield;
    }
});

Tdef(\melody, {
    var delta = 0.25;
    var scale = (Scale.major.degrees + 60).midicps;
    var step = 0;
    var asc = true;
    loop {
        Server.default.bind {
            Synth(\marimba, [
                \freq: scale[step],
                \atk: 0.01,
                \rel: 0.1,
                \sus: 0.1,
                \amp: rrand(0.3,0.5)
            ]);
        };
        step = (step + 1) % scale.size;
        delta.yield;
    }
});

Tdef(\example2, {
    var t = TempoClock(120/60);
    var delta = t.beatDur;
    Tdef(\clapPattern).play(t);
    Tdef(\kickPattern).play(t);
    //Tdef(\melody).play(t);
    loop {
        if ( t.beats % 4 == 0) {
            Tdef(\kickPattern).reset(t);
        };
        if ( t.beats % 12 == 0) {
            Server.default.bind {
                Synth(\tick, [pan: rrand(-1,1)])
            };
        };
        delta.yield;
    }
});

Tdef(\example2).play;
