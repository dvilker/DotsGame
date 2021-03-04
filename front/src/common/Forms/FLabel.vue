<template>
    <label :class="$cs.FLabel">
        <slot name="captionEx"><div :class="$cs.FCaption"><slot name="caption">{{ caption }}:</slot></div></slot>
        <slot/>
        <slot name="notesEx"><div :class="$cs.FNotes"><slot name="notes">{{ notes }}</slot></div></slot>
        <FError/>
    </label>
</template>

<script>
import FError from "./form/FError";
export default {
    components: {FError},
    props: {
        caption: null,
        notes: null,
    }
}
</script>
