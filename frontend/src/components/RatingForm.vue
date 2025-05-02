<template>
    <form @submit.prevent="submitRating" class="rating-form">
      <div class="rating-input-group">
        <input
          type="number"
          v-model="noteInput"
          placeholder="Note (0-10)"
          class="rating-input"
          :min="0"
          :max="10"
        />
        <button type="submit" class="rating-button">Noter</button>
      </div>
    </form>
  </template>
  
  <script>
  export default {
    name: 'RatingForm',
    props: {
      productId: {
        type: [Number, String],
        required: true
      },
      currentNote: {
        type: [Number, String],
        default: 'Aucune note disponible.'
      }
    },
    data() {
      return {
        noteInput: ''
      };
    },
    emits: ['add-note'],
    methods: {
      submitRating() {
        if (!this.noteInput) {
          alert('Veuillez entrer une note');
          return;
        }
        
        this.$emit('add-note', {
          productId: this.productId,
          noteInput: this.noteInput,
          currentNote: this.currentNote
        });
        
        this.noteInput = '';
      }
    }
  };
  </script>
  
  <style scoped>
  .rating-form {
    margin-top: 5px;
    width: 100%;
  }
  
  .rating-input-group {
    display: flex;
    gap: 8px;
    width: 100%;
  }
  
  .rating-input {
    flex: 1;
    padding: 8px 10px;
    border: 1px solid #ddd;
    border-radius: 6px;
    font-family: 'PP Formula', sans-serif;
    font-size: 0.9rem;
  }
  
  .rating-input:focus {
    border-color: #5AAAD8;
    outline: none;
  }
  
  .rating-button {
    padding: 8px 15px;
    border: none;
    background-color: #0E100E;
    color: white;
    border-radius: 6px;
    cursor: pointer;
    font-family: 'PP Formula', sans-serif;
    font-size: 0.9rem;
    transition: background-color 0.2s;
  }
  
  .rating-button:hover {
    background-color: #5AAAD8;
  }
  
  .rating-button:active {
    transform: scale(0.95);
  }
  </style>