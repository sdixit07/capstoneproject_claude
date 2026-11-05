import { isNextDisabled, isPrevDisabled, pageLabel } from './paginationState'

function Pagination({ page, totalPages, disabled, onPrev, onNext }) {
  return (
    <div className='pagination-bar' data-testid='pagination'>
      <button
        type='button'
        className='btn btn-outline-primary'
        data-testid='pagination-prev'
        disabled={isPrevDisabled(page, totalPages, disabled)}
        onClick={onPrev}
      >
        Prev
      </button>
      {totalPages > 0 && (
        <span className='pagination-label' data-testid='pagination-label'>
          {pageLabel(page, totalPages)}
        </span>
      )}
      <button
        type='button'
        className='btn btn-outline-primary'
        data-testid='pagination-next'
        disabled={isNextDisabled(page, totalPages, disabled)}
        onClick={onNext}
      >
        Next
      </button>
    </div>
  )
}

export default Pagination
