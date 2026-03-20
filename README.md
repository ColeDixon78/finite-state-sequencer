# Stochastic Sequencing

I had the idea of using stochastic matrices to control a synthesizer when first learning about Markov Chains in a computer science class I took in undergrad.
After doing a little bit of research, I can confidently say that I am not the first person to have this idea,
but I couldn't actually find any code examples in supercollider which I thought was surprising.

I am quite pleased with the results.
My worry was that it wouldn't be noticably different from just picking values according to a probability distribution.
However, I have found that messing with the transition matrix really does give you meaningful control over the randomness.
 
## Example 1
This example puts a very simple drum pattern underneath a droning bass synth whose pitch is controlled by a stochastic matrix.
Additionally, there are randomly generated 4 beat melodies being triggered periodically.
I spent a lot of time just tweaking values to control pitch and duration, and it dramatically changes the character of the melodies being generated.
