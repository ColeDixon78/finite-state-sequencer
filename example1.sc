(
SynthDef.new(name:\simple, ugenGraphFunc:{
    | out, freq = 440, pan = 0, dur = 1, atk = 0.1, dec = -4 |
    var source = 
        Saw.ar(freq) *
        Env.perc( atk, dur, curve:dec).ar(doneAction: Done.freeSelf);
    var filtered = BPF.ar(
        source,
        LFNoise0.kr(1,500, 1000),
        0.01
    );
    var panned = Pan2.ar(filtered, pan);
    Out.ar(bus:out, channelsArray:panned)
}, rates:nil, prependArgs:nil, variants:nil, metadata:nil).add;
)

(
r = p({
    | in |
    var currentStateIndex = 0;
    var states = in[0];
    var stateCount = states.size;
    var transitionMatrix = in[1];
    var indexArray = Array.series(stateCount,0,1);
    loop {
        currentStateIndex = indexArray.wchoose(transitionMatrix[currentStateIndex]);
        states[currentStateIndex].yield;
    };
});

p = Routine(
    {
        var deltas = [1/4,1/2,1];
        var deltaProbs = [
            [0.5,0.3,0.2],
            [0.3,0.6,0.1],
            [0.4,0.4,0.2],
        ];
        var deltaStream = r.asStream;
        var pitchStream = r.asStream;
        var pitches = (Scale.minor.degrees + 60).midicps;
        var pitchProbs = [
            [0.1,0.3,0.3,0,0.3,0,0],
            [0,0.1,0.9,0,0,0,0],
            [0.1,0,0.1,0.3,0.5,0,0],
            [0,0,0,0.1,0.9,0,0],
            [0.5,0,0,0,0.1,0.4,0],
            [0,0,0.4,0,0.2,0.1,0.3],
            [0.9,0,0,0,0,0.1,0],
        ];
        loop {
            Synth(\simple, [
                \freq, pitchStream.next([pitches,pitchProbs]),
                \pan, 1.0.rand2,
                \dec, 8.0.rand * -1,
                \dur, 2,
                \atk, 1.0.rand
            ]);
            deltaStream.next([deltas, deltaProbs]).yield;
        }
    }
);

p.play;
)

