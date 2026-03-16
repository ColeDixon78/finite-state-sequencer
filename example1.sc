//Ideas:
// counterpoint harmony with bass synth using pairs of notes as states
// add a high frequency sound that has reveres env shape for bass (fade in a high harmonic)
// add a transient to the bass
(
SynthDef.new(name:\simple, ugenGraphFunc:{
    | out, freq = 440, pan = 0, dur = 1, atk = 0.1, dec = -4, amp = 0.5, width = 0.1 |
    var pitchMod = SinOsc.kr(
        freq: LFNoise0.kr(10,5),
        mul: 2
    );
    var source = 
        Saw.ar(freq + pitchMod, amp * 0.7) *
        Env.perc( atk, dur, curve:dec).kr(doneAction: Done.freeSelf);
    var filtered = BPF.ar(
        source,
        freq * Rand(1,5).floor,
        XLine.kr(width,0.01,dur/2),
    );
    var panned = Pan2.ar(filtered, pan);
    Out.ar(bus:out, channelsArray:panned)
}, rates:nil, prependArgs:nil, variants:nil, metadata:nil).add;

SynthDef.new(name:\bass, ugenGraphFunc:{
    | out, freq = 440, atk = 0.1, sus = 0.5, rel = 1, amp = 1, gate = 1, pan = 0 |
    var source = LPF.ar(
        (
            SinOsc.ar(
                freq: freq - LFNoise0.kr(LFTri.kr(1,mul:30), 3),
                mul: 0.25
            ) +
            SinOsc.ar(freq: freq + SinOsc.ar(freq * 1.2,mul: 40), mul: 0.25) +
            SinOsc.ar(freq: freq + SinOsc.ar(freq * 2,mul: 100), mul:  0.25)
        )
        * 
        Env.linen(
            atk,
            sus,
            rel,
            amp
        ).ar(doneAction:Done.freeSelf, gate: gate),
        freq + Line.kr(0, freq * 4, atk * 3)
    );
    var panned = Pan2.ar(source , pan + LFTri.kr(1, mul: 0.2));
    Out.ar(bus:out, channelsArray:panned)
}, rates:nil, prependArgs:nil, variants:nil, metadata:nil).add;
)

(
// Generate a random 4 beat melody
Tdef(\melody,
    {
        var stochasticPattern = p({
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
        var deltas = [1/4,1/2,1,2];
        var deltaProbs = [
            [0.1,0,0.8,0.1],
            [0.5,0.2,0.3,0],
            [0.4,0.4,0.2,0],
            [0.5,0.5,0,0],
        ];
        var deltaStream = stochasticPattern.asStream;
        var pitchStream = stochasticPattern.asStream;
        var pitches = (Scale.major.degrees + 53).midicps;

        // Pitches will transition randomly from one state to the next
        var pMat1 = [
            [0.1,0.3,0.3,0,0.3,0,0],
            [0,0.1,0.9,0,0,0,0],
            [0.1,0,0.1,0.3,0.5,0,0],
            [0,0,0,0.1,0.9,0,0],
            [0.5,0,0,0,0.1,0.4,0],
            [0,0,0.4,0,0.2,0.1,0.3],
            [0.9,0,0,0,0,0.1,0],
        ];

        // Pitches are completely deterministic - ascending the scale
        var pMat2 = Array.fill2D(
            pitches.size,
            pitches.size,
            { | r, c | if ( c == (r + 1 % pitches.size) , 1,0) }
        );
        var nextPitch;
        var nextDelta;
        var totalDur = 0;
        while ({totalDur < 4}) {
            nextPitch = pitchStream.next([pitches,pMat1, pitches.size.rand]);
            Synth(\simple, [
                \freq, nextPitch,
                \pan, 1.0.rand2,
                \dec, -8,
                \dur, 4,
                \atk, 0.01,
                \width, 0.2,
                \amp, rrand(0.3,0.6)
            ]);
            Synth(\simple, [
                \freq, nextPitch * [2,3,6].choose,
                \pan, 1.0.rand2,
                \dec, -2,
                \dur, 2,
                \atk, 0.1,
                \width, 0.2,
                \amp, rrand(0.1,0.4)
            ]);
            nextDelta = deltaStream.next([deltas, deltaProbs, deltas.size.rand]);
            totalDur = totalDur + nextDelta;
            nextDelta.yield;
        }
    }
);
)

(
Routine({
    var delta = 4;
    loop {
        Tdef(\melody).play;
        Synth(\bass, [
            \freq: (Scale.major.degrees + 41).choose.midicps,
            \amp: 1,
            \atk: rrand(0.1,0.4),
            \rel: rrand(1,2),
            \sus: rrand(2,3)
        ]);
        delta.yield;
    }
}).play;
)

LevelIndicator.meterServer(s)
[24,23].size.rand
