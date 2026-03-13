(
SynthDef.new(name:\simple, ugenGraphFunc:{
    | out, freq = 440|
    var source = SinOsc.ar(freq) * Env.perc(0.1,0.3,curve:-8).ar(doneAction: Done.freeSelf);
    Out.ar(bus:out, channelsArray:source)
}, rates:nil, prependArgs:nil, variants:nil, metadata:nil).add;

r = p({
    | in |
    var currentStateIndex = 0;
    var states = in[0];
    var stateCount = states.size;
    var transitionMatrix = in[1];
    var indexArray = Array.series(stateCount,0,1);
    in.postln;
    loop {
        currentStateIndex = indexArray.wchoose(transitionMatrix[currentStateIndex]);
        states[currentStateIndex].yield;
    };
});

p = Routine(
    {
        var delta = 1/4;
        var pitchStream = r.asStream;
        var pitches = [440,880];
        var pitchProbs = [[0.1,0.9],[1,0]];
        loop {
            Synth(\simple, [\freq, pitchStream.next([pitches,pitchProbs])]);
            delta.yield;
        }
    }
);

p.play;
)
