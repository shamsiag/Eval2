<template>
    <div class="rating-bar">
      <div class="stars-container">
        <span 
          v-for="i in 5" 
          :key="i" 
          class="star"
          :class="{ 'filled': (rating / 2) >= i, 'half-filled': isHalfStar(i) }"
        >
          ★
        </span>
      </div>
      <span class="rating-text" v-if="typeof rating === 'number'">
        {{ (rating / 2).toFixed(1) }}/5
      </span>
      <span class="rating-text" v-else>
        Pas encore noté
      </span>
    </div>
  </template>
  
  <script>
  export default {
    name: 'RatingBar',
    props: {
      rating: {
        type: [Number, String],
        default: 0
      }
    },
    methods: {
      isHalfStar(position) {
        // La note est sur 10, donc on la divise par 2 pour l'afficher sur 5
        const normalizedRating = typeof this.rating === 'number' ? this.rating / 2 : 0;
        return Math.ceil(normalizedRating) === position && normalizedRating % 1 !== 0;
      }
    }
  };
  </script>
  
  <style scoped>
  .rating-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 5px 0;
  }
  
  .stars-container {
    display: flex;
  }
  
  .star {
    font-size: 1.2rem;
    color: #ddd;
    position: relative;
  }
  
  .star.filled {
    color: #FFD700;
  }
  
  .star.half-filled {
    position: relative;
  }
  
  .star.half-filled:before {
    content: '★';
    position: absolute;
    color: #FFD700;
    width: 50%;
    overflow: hidden;
  }
  
  .rating-text {
    font-size: 0.9rem;
    font-family: 'PP Formula', sans-serif;
    color: #666;
  }
  </style>