# Safe local agent planning

The Android assistant must provide an offline, persistent, reviewable plan for a user goal. A plan contains ordered tasks with dependencies and cannot execute external actions. Users can create, view, complete, and reset a plan using chat commands. Completion is blocked until prerequisites are complete. The feature must store only locally on the device and retain the existing approval policy for consequential actions.
